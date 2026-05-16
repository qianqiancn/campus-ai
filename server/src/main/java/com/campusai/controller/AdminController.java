package com.campusai.controller;

import com.campusai.model.User;
import com.campusai.repository.ConversationRepository;
import com.campusai.repository.FeedbackRepository;
import com.campusai.repository.KnowledgeRepository;
import com.campusai.repository.MessageRepository;
import com.campusai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final FeedbackRepository feedbackRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        long userCount = userRepository.count();
        long knowledgeCount = knowledgeRepository.count();
        long conversationCount = conversationRepository.count();

        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayMessages = messageRepository.countByCreatedAtBetween(
                todayStart, todayStart.plusDays(1));
        long todayQuestions = messageRepository.countUserMessagesBetween(
                todayStart, todayStart.plusDays(1));

        return ResponseEntity.ok(Map.of(
                "userCount", userCount,
                "knowledgeCount", knowledgeCount,
                "conversationCount", conversationCount,
                "todayMessages", todayMessages,
                "todayQuestions", todayQuestions
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "studentId", u.getStudentId(),
                        "username", u.getUsername(),
                        "role", u.getRole(),
                        "enabled", u.getEnabled(),
                        "createdAt", u.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (body.containsKey("enabled")) {
            user.setEnabled((Boolean) body.get("enabled"));
        }
        if (body.containsKey("password") && body.get("password") != null
                && !body.get("password").toString().isBlank()) {
            user.setPassword(passwordEncoder.encode(body.get("password").toString()));
        }
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "更新成功"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if ("ADMIN".equals(user.getRole())) {
            return ResponseEntity.badRequest().body(Map.of("message", "不能删除管理员"));
        }
        userRepository.delete(user);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/stats/daily")
    public ResponseEntity<?> dailyStats(@RequestParam(defaultValue = "7") int days) {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            long messages = messageRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long questions = messageRepository.countUserMessagesBetween(dayStart, dayEnd);

            result.add(Map.of(
                    "date", date.toString(),
                    "messages", messages,
                    "questions", questions
            ));
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/category")
    public ResponseEntity<?> categoryStats() {
        List<Object[]> raw = knowledgeRepository.countByCategory();
        List<Map<String, Object>> result = raw.stream()
                .map(row -> Map.of("name", row[0], "value", row[1]))
                .toList();
        return ResponseEntity.ok(result);
    }

    @Transactional(readOnly = true)
    @GetMapping("/feedbacks")
    public ResponseEntity<?> listFeedbacks() {
        var feedbacks = feedbackRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> result = feedbacks.stream().map(f -> {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("id", f.getId());
            map.put("messageId", f.getMessage().getId());
            map.put("messageContent", f.getMessage().getContent().length() > 80
                    ? f.getMessage().getContent().substring(0, 80) + "..."
                    : f.getMessage().getContent());
            map.put("userId", f.getUser().getId());
            map.put("username", f.getUser().getUsername());
            map.put("type", f.getType());
            map.put("createdAt", f.getCreatedAt());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/feedbacks/stats")
    public ResponseEntity<?> feedbackStats() {
        long total = feedbackRepository.count();
        long likes = feedbackRepository.countByType("LIKE");
        long dislikes = feedbackRepository.countByType("DISLIKE");
        return ResponseEntity.ok(Map.of("total", total, "likes", likes, "dislikes", dislikes));
    }
}
