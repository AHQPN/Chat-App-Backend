package org.example.chatapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "message_reactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // message_id -> messages.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    // conversation_member_id -> conversation_members.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_member_id", nullable = false)
    private ConversationMember conversationMember;

    @Column(nullable = false, length = 200)
    private String emoji;

    @Column(name = "reacted_at")
    private Long reactedAt;
}
