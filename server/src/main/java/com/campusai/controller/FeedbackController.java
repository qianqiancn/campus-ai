package com.campusai.controller;

import com.campusai.model.Feedback;
import com.campusai.model.Message;
import com.campusai.model.User;
import com.campusai.repository.FeedbackRepository;
import com.campusai.repository.MessageRepository;
import com.campusai.service.UserService;
import com.campusai.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> submit(@RequestBody Map<String, Object> body,
                                    @RequestHeader("Authorization") String authHeader) {
        try {
            String studentId = jwtUtil.getStudentIdFromToken(authHeader.substring(7));
            User user = userService.findByStudentId(studentId);

            Long messageId = Long.valueOf(body.get("messageId").toString());
            String type = body.get("type").toString();

            Message message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("消息不存在"));

            if (feedbackRepository.existsByMessageIdAndUserId(messageId, user.getId())) {
                feedbackRepository.findByMessageIdAndUserId(messageId, user.getId())
                        .ifPresent(feedbackRepository::delete);
                return ResponseEntity.ok(Map.of("message", "反馈已取消"));
            }

            Feedback feedback = Feedback.builder()
                    .message(message)
                    .user(user)
                    .type(type)
                    .build();
            feedbackRepository.save(feedback);
            return ResponseEntity.ok(Map.of("message", "反馈成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "反馈失败: " + e.getMessage()));
        }
    }
}
