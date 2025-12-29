package org.example.chatapp.repository;

import org.example.chatapp.entity.HiddenMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HiddenMessageRepository extends JpaRepository<HiddenMessage, Integer> {
    
    @org.springframework.data.jpa.repository.Query("SELECT h.message.id FROM HiddenMessage h WHERE h.user.userId = :userId")
    List<Integer> findHiddenMessageIdsByUserId(Integer userId);
    
    boolean existsByUser_UserIdAndMessage_Id(Integer userId, Integer messageId);
}
