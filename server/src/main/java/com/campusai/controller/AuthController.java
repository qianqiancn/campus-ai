package com.campusai.controller;

import com.campusai.dto.LoginRequest;
import com.campusai.dto.LoginResponse;
import com.campusai.dto.RegisterRequest;
import com.campusai.model.User;
import com.campusai.repository.ConversationRepository;
import com.campusai.repository.MessageRepository;
import com.campusai.service.UserService;
import com.campusai.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            User user = userService.login(request.getStudentId(), request.getPassword());
            String token = jwtUtil.generateToken(user.getStudentId(), user.getRole());
            return ResponseEntity.ok(new LoginResponse(
                    token, user.getUsername(), user.getRole(), user.getStudentId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(
                    request.getStudentId(), request.getUsername(), request.getPassword()
            );
            String token = jwtUtil.generateToken(user.getStudentId(), user.getRole());
            return ResponseEntity.ok(new LoginResponse(
                    token, user.getUsername(), user.getRole(), user.getStudentId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(401).body(Map.of("message", "Token无效"));
            }
            String studentId = jwtUtil.getStudentIdFromToken(token);
            User user = userService.findByStudentId(studentId);
            return ResponseEntity.ok(Map.of(
                    "studentId", user.getStudentId(),
                    "username", user.getUsername(),
                    "role", user.getRole(),
                    "avatar", user.getAvatar() != null ? user.getAvatar() : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("message", "认证失败"));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> body,
                                            @RequestHeader("Authorization") String authHeader) {
        try {
            String studentId = jwtUtil.getStudentIdFromToken(authHeader.substring(7));
            User user = userService.findByStudentId(studentId);
            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");

            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("message", "新密码至少6位"));
            }

            userService.changePassword(user, oldPassword, newPassword);
            return ResponseEntity.ok(Map.of("message", "密码修改成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "修改失败，请稍后重试"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader("Authorization") String authHeader) {
        try {
            String studentId = jwtUtil.getStudentIdFromToken(authHeader.substring(7));
            User user = userService.findByStudentId(studentId);
            long conversationCount = conversationRepository.countByUserId(user.getId());
            long messageCount = messageRepository.countByConversationUserId(user.getId());
            return ResponseEntity.ok(Map.of(
                    "conversationCount", conversationCount,
                    "messageCount", messageCount
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "conversationCount", 0,
                    "messageCount", 0
            ));
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestBody Map<String, String> body,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            String studentId = jwtUtil.getStudentIdFromToken(authHeader.substring(7));
            User user = userService.findByStudentId(studentId);
            String avatar = body.get("avatar");
            if (avatar == null || avatar.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "头像数据不能为空"));
            }
            user.setAvatar(avatar);
            userService.save(user);
            return ResponseEntity.ok(Map.of("message", "头像更新成功", "avatar", avatar));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "头像上传失败: " + e.getMessage()));
        }
    }
}
