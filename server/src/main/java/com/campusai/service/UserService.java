package com.campusai.service;

import com.campusai.model.User;
import com.campusai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(String studentId, String username, String password) {
        if (userRepository.existsByStudentId(studentId)) {
            throw new RuntimeException("该学号已注册");
        }
        User user = User.builder()
                .studentId(studentId)
                .username(username)
                .password(passwordEncoder.encode(password))
                .role("STUDENT")
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    public User login(String studentId, String password) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("学号或密码错误"));
        if (!user.getEnabled()) {
            throw new RuntimeException("账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("学号或密码错误");
        }
        return user;
    }

    public User findByStudentId(String studentId) {
        return userRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public void changePassword(User user, String oldPassword, String newPassword) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public User save(User user) {
        return userRepository.save(user);
    }
}
