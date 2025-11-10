package org.example.chatapp.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.Data;
import org.example.chatapp.service.enums.ConversationEnum;

import java.util.Set;

@Entity
@Data
@Table(name = "conversations")
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ConversationEnum type;

    @Column(name = "is_private", columnDefinition = "TINYINT(1) DEFAULT 0", nullable = false)
    private Boolean isPrivate; // is_private TINYINT(1)

    @Column(name = "created_at")
    private Long createdAt; // created_at BIGINT

    // --- Mối quan hệ (Relationships) ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    @JsonBackReference
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonBackReference
    private User creator;

    // Quan hệ 1:N với ConversationMember
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<ConversationMember> members;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Message> messages;

}
