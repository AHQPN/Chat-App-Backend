package org.example.chatapp.repository;

import org.example.chatapp.entity.Conversation;
import org.example.chatapp.entity.ConversationMember;
import org.example.chatapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember,Integer> {
    Boolean existsByConversationAndUser(Conversation conversation, User user);

    Optional<ConversationMember> findByConversation_IdAndUser_UserId(Integer conversationId, Integer userUserId);
}
