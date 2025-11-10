package org.example.chatapp.repository;

import org.example.chatapp.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface ConversationRepository extends JpaRepository<Conversation,Integer> {
    Optional<Conversation> getConversationById(Integer id);
}
