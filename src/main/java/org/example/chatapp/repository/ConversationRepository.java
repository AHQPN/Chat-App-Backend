package org.example.chatapp.repository;

import org.example.chatapp.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ConversationRepository extends JpaRepository<Conversation,Integer> {
    Optional<Conversation> getConversationById(Integer id);
    
    @Query("SELECT c FROM Conversation c " +
           "WHERE c.workspace.id = :workspaceId " +
           "AND c.isPrivate = false " +
           "AND c.type = org.example.chatapp.service.enums.ConversationEnum.CHANNEL " +
           "AND c.id NOT IN (SELECT m.conversation.id FROM ConversationMember m WHERE m.user.userId = :userId)")
    List<Conversation> findPublicChannelsNotJoined(
            @Param("workspaceId") Integer workspaceId,
            @Param("userId") Integer userId);

    List<Conversation> findAllByWorkspaceIdIn(List<Integer> workspaceIds);
}
