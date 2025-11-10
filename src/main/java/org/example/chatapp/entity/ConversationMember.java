package org.example.chatapp.entity;


import jakarta.persistence.*;
import lombok.Data;
import org.example.chatapp.service.enums.WorkspaceRoleEnum;

@Entity
@Data
@Table(name = "conversation_members")
public class ConversationMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "joined_at")
    private Long joinedAt;

    @Column(name = "last_read_at")
    private Long lastReadAt;

    @Column(name = "is_notif_enabled", columnDefinition = "TINYINT(1) DEFAULT 1", nullable = false)
    private Boolean isNotifEnabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", columnDefinition = "ENUM('ADMIN','MEMBER','GUEST') DEFAULT 'MEMBER'")
    private WorkspaceRoleEnum role;

    // conversation_id INT (FK -> conversations.id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    // user_id INT (FK -> users.user_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private Message lastReadMessage;
}
