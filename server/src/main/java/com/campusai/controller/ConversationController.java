package com.campusai.controller;

import com.campusai.model.Conversation;
import com.campusai.model.Message;
import com.campusai.model.User;
import com.campusai.repository.ConversationRepository;
import com.campusai.repository.FeedbackRepository;
import com.campusai.repository.MessageRepository;
import com.campusai.service.UserService;
import com.campusai.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final FeedbackRepository feedbackRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    private String getStudentId(String authHeader) {
        return jwtUtil.getStudentIdFromToken(authHeader.substring(7));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestHeader("Authorization") String authHeader) {
        String studentId = getStudentId(authHeader);
        User user = userService.findByStudentId(studentId);
        var conversations = conversationRepository.findByUserIdOrderByUpdatedAtDesc(user.getId())
                .stream()
                .map(c -> Map.of(
                        "id", c.getId(),
                        "title", c.getTitle(),
                        "createdAt", c.getCreatedAt(),
                        "updatedAt", c.getUpdatedAt()
                ))
                .toList();
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMessages(@PathVariable Long id,
                                         @RequestHeader("Authorization") String authHeader) {
        String studentId = getStudentId(authHeader);
        User user = userService.findByStudentId(studentId);
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("对话不存在"));
        if (!conversation.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "无权访问"));
        }
        var messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(id).stream()
                .map(m -> {
                    String feedback = feedbackRepository.findByMessageIdAndUserId(m.getId(), user.getId())
                            .map(f -> f.getType())
                            .orElse(null);
                    return Map.of(
                            "id", m.getId(),
                            "role", m.getRole(),
                            "content", m.getContent(),
                            "createdAt", m.getCreatedAt(),
                            "feedback", feedback != null ? feedback : ""
                    );
                })
                .toList();
        return ResponseEntity.ok(Map.of(
                "conversation", Map.of("id", conversation.getId(), "title", conversation.getTitle()),
                "messages", messages
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @RequestHeader("Authorization") String authHeader) {
        String studentId = getStudentId(authHeader);
        User user = userService.findByStudentId(studentId);
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("对话不存在"));
        if (!conversation.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("message", "无权操作"));
        }
        conversationRepository.delete(conversation);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }
}
