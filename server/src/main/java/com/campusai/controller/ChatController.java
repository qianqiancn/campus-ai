package com.campusai.controller;

import com.campusai.dto.ChatRequest;
import com.campusai.dto.ChatResponse;
import com.campusai.model.Conversation;
import com.campusai.model.Knowledge;
import com.campusai.model.Message;
import com.campusai.model.User;
import com.campusai.service.ChatService;
import com.campusai.service.KnowledgeService;
import com.campusai.service.OllamaService;
import com.campusai.service.UserService;
import com.campusai.util.JwtUtil;
import com.campusai.repository.ConversationRepository;
import com.campusai.repository.MessageRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final OllamaService ollamaService;
    private final JwtUtil jwtUtil;
    private final KnowledgeService knowledgeService;
    private final UserService userService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @PostMapping
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request,
                                  @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String studentId = jwtUtil.getStudentIdFromToken(token);

        try {
            ChatResponse response = chatService.chat(
                    studentId, request.getQuestion(), request.getConversationId()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Transactional
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request,
                                 @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String studentId = jwtUtil.getStudentIdFromToken(token);

        SseEmitter emitter = new SseEmitter(300000L);

        try {
            User user = userService.findByStudentId(studentId);
            List<Knowledge> relevantKnowledge = knowledgeService.search(request.getQuestion());
            List<Knowledge> topKnowledge = relevantKnowledge.stream().limit(5).collect(Collectors.toList());

            String context = buildContext(topKnowledge);
            String systemPrompt = """
                    你是四川工商职业技术学院的校园智能客服助手。你的职责是帮助学生解答关于学校的各类问题。
                    
                    回答规则:
                    1. 优先使用下面提供的【知识库参考内容】来回答问题
                    2. 如果知识库中有相关内容，请基于知识库内容来回答，保持回答准确、有条理
                    3. 如果知识库中没有相关内容，请根据你的知识给出合理的引导性回复
                    4. 保持友好、热情的语气，但不要每条回复都加表情符号，只在适当的时候偶尔使用
                    5. 回答使用中文，简洁明了，不要使用markdown格式
                    """;

            AtomicReference<String> fullAnswer = new AtomicReference<>("");
            AtomicReference<Conversation> conversationRef = new AtomicReference<>();
            AtomicBoolean emitterCompleted = new AtomicBoolean(false);

            emitter.onCompletion(() -> emitterCompleted.set(true));
            emitter.onTimeout(() -> emitterCompleted.set(true));

            Conversation conversation = getOrCreateConversation(user, request.getQuestion(), request.getConversationId());
            conversationRef.set(conversation);
            saveMessage(conversation, "USER", request.getQuestion());

            log.info("streamChat: conversationId={}, requestConversationId={}", conversation.getId(), request.getConversationId());
            String historyContext = buildHistoryContext(conversation);
            log.info("streamChat: historyContext length={}, isEmpty={}", historyContext.length(), historyContext.isEmpty());

            String prompt;
            if (!historyContext.isEmpty()) {
                prompt = historyContext + "\n" + systemPrompt + """
                    
                    【知识库参考内容】
                    %s
                    
                    【学生当前问题】
                    %s
                    
                    请结合对话历史和知识库内容回答学生的问题:
                    """.formatted(context, request.getQuestion());
            } else {
                prompt = systemPrompt + """
                    
                    【知识库参考内容】
                    %s
                    
                    【学生问题】
                    %s
                    
                    请根据以上信息回答学生的问题:
                    """.formatted(context, request.getQuestion());
            }

            String sources = topKnowledge.stream()
                    .map(k -> "[" + k.getCategory() + "] " + k.getQuestion())
                    .collect(Collectors.joining("; "));

            emitter.send(SseEmitter.event().name("init").data(Map.of("conversationId", conversation.getId())));

            ollamaService.generateStream(prompt,
                    chunk -> {
                        if (emitterCompleted.get()) return;
                        try {
                            fullAnswer.updateAndGet(a -> a + chunk);
                            emitter.send(SseEmitter.event().name("token").data(chunk));
                        } catch (Exception e) {
                            emitterCompleted.set(true);
                        }
                    },
                    () -> {
                        if (emitterCompleted.get()) return;
                        try {
                            Message aiMessage = saveMessage(conversationRef.get(), "AI", fullAnswer.get());
                            emitter.send(SseEmitter.event().name("done").data(
                                Map.of("messageId", aiMessage.getId(), "conversationId", conversationRef.get().getId())
                            ));
                            Thread.sleep(200);
                            emitter.complete();
                        } catch (Exception e) {
                            emitterCompleted.set(true);
                            try { emitter.complete(); } catch (Exception ignored) {}
                        }
                    },
                    error -> {
                        if (emitterCompleted.get()) return;
                        try {
                            emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                            emitter.complete();
                        } catch (Exception ex) {
                            emitterCompleted.set(true);
                            try { emitter.complete(); } catch (Exception ignored) {}
                        }
                    }
            );

        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private String buildContext(List<Knowledge> knowledgeList) {
        if (knowledgeList.isEmpty()) return "暂无相关知识库内容。";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < knowledgeList.size(); i++) {
            Knowledge k = knowledgeList.get(i);
            sb.append("【知识").append(i + 1).append("】\n");
            sb.append("问题: ").append(k.getQuestion()).append("\n");
            sb.append("答案: ").append(k.getAnswer()).append("\n\n");
        }
        return sb.toString();
    }

    private String buildHistoryContext(Conversation conversation) {
        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        log.info("buildHistoryContext: conversationId={}, historySize={}", conversation.getId(), history.size());
        if (history.size() <= 1) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("【以下是你们之前的对话历史，你必须结合这些上下文来回答学生的最新问题，不要忘记之前说过的话】\n");
        int count = 0;
        int total = history.size() - 1;
        for (int i = 0; i < total && count < 10; i++) {
            Message m = history.get(i);
            String role = m.getRole().equals("USER") ? "学生" : "助手";
            String shortContent = m.getContent().length() > 200
                    ? m.getContent().substring(0, 200) + "..."
                    : m.getContent();
            sb.append(role).append(": ").append(shortContent).append("\n");
            count++;
        }
        sb.append("\n");
        return sb.toString();
    }

    private Conversation getOrCreateConversation(User user, String question, Long conversationId) {
        if (conversationId != null) {
            return conversationRepository.findById(conversationId)
                    .orElseGet(() -> createConversation(user, question));
        }
        return createConversation(user, question);
    }

    private Conversation createConversation(User user, String question) {
        String title = question.length() > 30 ? question.substring(0, 30) + "..." : question;
        Conversation conversation = Conversation.builder()
                .user(user)
                .title(title)
                .build();
        return conversationRepository.save(conversation);
    }

    private Message saveMessage(Conversation conversation, String role, String content) {
        Message message = Message.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .build();
        Message saved = messageRepository.save(message);
        conversation.setUpdatedAt(java.time.LocalDateTime.now());
        conversationRepository.save(conversation);
        return saved;
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        boolean available = ollamaService.isAvailable();
        return ResponseEntity.ok(Map.of("ollamaAvailable", available));
    }
}
