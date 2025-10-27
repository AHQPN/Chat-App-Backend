package org.example.chatapp.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Entity
@Data
@Table(name = "workspaces")
public class Workspace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name; // name VARCHAR(255)

    @Column(name = "created_at")
    private Long createdAt; // created_at BIGINT

    // --- Mối quan hệ (Relationships) ---

    // created_by INT (FK -> users.user_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonBackReference
    private User creator;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<WorkspaceMember> members;

    @OneToMany(mappedBy = "workspace", cascade = CascadeType.ALL)
    @JsonManagedReference
    private Set<Conversation> conversations;
}
