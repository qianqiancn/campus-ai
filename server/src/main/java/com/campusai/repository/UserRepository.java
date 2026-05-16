package com.campusai.repository;

import com.campusai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
}
