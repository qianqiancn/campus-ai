package com.campusai.repository;

import com.campusai.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByConversationUserId(Long userId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.role = 'USER' AND m.createdAt BETWEEN :start AND :end")
    long countUserMessagesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
