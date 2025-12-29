package org.example.chatapp.repository;

import org.example.chatapp.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message,Integer> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Integer conversationId);


    @Query("SELECT COUNT(m) FROM Message m " +
            "WHERE m.conversation.id = :conversationId " +
            "AND m.id > :lastReadMessageId " +
            "AND m.sender.userId != :userId " +
            "AND m.status != 'REVOKED'")
    Integer countUnreadMessages(
            @Param("conversationId") Integer conversationId,
            @Param("lastReadMessageId") Integer lastReadMessageId,
            @Param("userId") Integer userId
    );

    Page<Message> findByConversationIdAndThreadIsNull(Integer conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m " +
           "LEFT JOIN HiddenMessage h ON h.message.id = m.id AND h.user.userId = :userId " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.thread IS NULL " +
           "AND h.id IS NULL")
    Page<Message> findVisibleChannelMessages(@Param("conversationId") Integer conversationId,
                                             @Param("userId") Integer userId,
                                             Pageable pageable);

    Page<Message> findByThreadId(Integer threadId, Pageable pageable);

    @Query("SELECT m FROM Message m " +
           "LEFT JOIN HiddenMessage h ON h.message.id = m.id AND h.user.userId = :userId " +
           "WHERE m.thread.id = :threadId " + 
           "AND h.id IS NULL")
    Page<Message> findVisibleThreadMessages(@Param("threadId") Integer threadId,
                                            @Param("userId") Integer userId,
                                            Pageable pageable);

    @Transactional
    @Modifying
    @Query("UPDATE Message m SET m.threadReplyCount = m.threadReplyCount + 1 WHERE m.id = :threadId")
    void incrementThreadReplyCount(Integer threadId);
    
    @Transactional
    @Modifying
    @Query("UPDATE Message m SET m.threadReplyCount = m.threadReplyCount - 1 WHERE m.id = :threadId")
    void decrementThreadReplyCount(Integer threadId);

    @Query("SELECT COUNT(m) FROM Message m " +
           "LEFT JOIN HiddenMessage h ON h.message.id = m.id AND h.user.userId = :userId " +
           "WHERE m.conversation.id = :conversationId " +
           "AND m.thread IS NULL " +
           "AND h.id IS NULL " +
           "AND m.createdAt > :createdAt")
    Integer countVisibleMessagesAfter(@Param("conversationId") Integer conversationId,
                                   @Param("userId") Integer userId,
                                   @Param("createdAt") Long createdAt);

    @Query("SELECT m FROM Message m " +
           "LEFT JOIN HiddenMessage h ON h.message.id = m.id AND h.user.userId = :userId " +
           "WHERE m.conversation.id = :conversationId " +
           "AND h.id IS NULL " +
           "AND m.status != org.example.chatapp.service.enums.MessageStatus.REVOKED " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Message> searchMessages(@Param("conversationId") Integer conversationId,
                                 @Param("userId") Integer userId,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);
}
