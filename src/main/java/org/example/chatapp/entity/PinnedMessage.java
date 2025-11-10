package org.example.chatapp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pinned_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinnedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // message_id -> messages.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    // conversation_id -> conversations.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    // pinned_by_member_id -> conversation_members.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pinned_by_member_id", nullable = false)
    private ConversationMember pinnedBy;

    // BIGINT
    @Column(name = "pinned_at")
    private Long pinnedAt;
}
