package org.example.chatapp.repository;

import org.example.chatapp.entity.PinnedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PinnedMessageRepository extends JpaRepository<PinnedMessage, Integer> {


    Boolean existsByMessage_Id(Integer messageId);

    void deleteByMessage_Id(Integer messageId);
}
