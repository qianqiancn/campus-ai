package com.campusai.repository;

import com.campusai.model.Conversation;
import com.campusai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    long countByUserId(Long userId);
}
