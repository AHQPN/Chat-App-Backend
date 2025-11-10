package org.example.chatapp.repository;


import org.example.chatapp.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction,Integer> {
    Optional<MessageReaction> findByMessage_IdAndConversationMember_Id(Integer messageId, Integer memberId);

    Boolean existsByMessage_IdAndConversationMember_Id(Integer messageId, Integer memberId);

    void deleteByMessage_IdAndConversationMember_Id(Integer messageId, Integer memberId);
}
