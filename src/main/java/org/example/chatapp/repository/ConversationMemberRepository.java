package org.example.chatapp.repository;

import org.example.chatapp.entity.Conversation;
import org.example.chatapp.entity.ConversationMember;
import org.example.chatapp.entity.User;
import org.example.chatapp.service.enums.ConversationRoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember,Integer> {
    Boolean existsByConversationAndUser(Conversation conversation, User user);
    Boolean existsByIdAndConversation_Id(Integer memberId, Integer conversationId);

    Optional<ConversationMember> findConversationMemberByUser_UserId(Integer userUserId);
    Optional<ConversationMember> findByConversation_IdAndUser_UserId(Integer conversationId, Integer userUserId);

    @Query(
        "SELECT cm FROM ConversationMember cm " +
        "JOIN FETCH cm.user " +
        "WHERE cm.conversation.id = :conversationId AND cm.user.userId = :userId")
    Optional<ConversationMember> findByConversationIdAndUserIdWithUser(
        @Param("conversationId") Integer conversationId,
        @Param("userId") Integer userId);

    ConversationMember getConversationMembersById(Integer id);

    List<ConversationMember> findByUser_UserIdAndRoleNot(Integer userUserId, ConversationRoleEnum role);

    List<ConversationMember> findAllByConversation_Id(Integer conversationId);
    
    List<ConversationMember> findAllByConversation_IdAndRoleNot(Integer conversationId, org.example.chatapp.service.enums.ConversationRoleEnum role);

    @Query("SELECT cm.conversation.id FROM ConversationMember cm WHERE cm.user.userId = :userId")
    List<Integer> findConversationIdsByUserId(@org.springframework.data.repository.query.Param("userId") Integer userId);
    
    int countByConversation_Id(Integer conversationId);

    List<ConversationMember> findAllByConversation_IdInAndRoleNot(List<Integer> conversationIds, ConversationRoleEnum conversationRoleEnum);
}
