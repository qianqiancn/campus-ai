package com.campusai.repository;

import com.campusai.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderByCreatedAtDesc();
    long countByType(String type);
    boolean existsByMessageIdAndUserId(Long messageId, Long userId);
    Optional<Feedback> findByMessageIdAndUserId(Long messageId, Long userId);
}
