package org.example.chatapp.repository;

import org.example.chatapp.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message,Integer> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Integer conversationId);

    Page<Message> findByConversationIdOrderByCreatedAtDesc(Integer conversationId, Pageable pageable);
}
