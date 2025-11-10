package org.example.chatapp.repository;

import org.example.chatapp.entity.MessageMention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageMentionRepository extends JpaRepository<MessageMention,Integer> {
}
