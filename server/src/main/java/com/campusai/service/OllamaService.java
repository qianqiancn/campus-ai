package com.campusai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class OllamaService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.model}")
    private String model;

    public OllamaService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder().build();
    }

    public String generate(String prompt) {
        String url = baseUrl + "/api/generate";

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false
        );

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            return jsonNode.get("response").asText();
        } catch (Exception e) {
            log.error("Ollama调用失败: {}", e.getMessage());
            return "抱歉，AI服务暂时不可用，请稍后再试。错误信息: " + e.getMessage();
        }
    }

    public void generateStream(String prompt, Consumer<String> onToken, Runnable onComplete, Consumer<Exception> onError) {
        String url = baseUrl + "/api/generate";

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", true,
                "options", Map.of(
                        "temperature", 0.7,
                        "num_predict", 512,
                        "top_k", 40,
                        "top_p", 0.9
                )
        );

        new Thread(() -> {
            try {
                String json = objectMapper.writeValueAsString(requestBody);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) continue;
                        try {
                            JsonNode node = objectMapper.readTree(line);
                            String token = node.get("response").asText();
                            if (token != null && !token.isEmpty()) {
                                onToken.accept(token);
                            }
                            if (node.has("done") && node.get("done").asBoolean()) {
                                break;
                            }
                        } catch (Exception e) {
                            // skip malformed lines
                        }
                    }
                    onComplete.run();
                }
            } catch (Exception e) {
                log.debug("Ollama stream error (client may have disconnected): {}", e.getMessage());
                onError.accept(e);
            }
        }).start();
    }

    public boolean isAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/api/tags", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
