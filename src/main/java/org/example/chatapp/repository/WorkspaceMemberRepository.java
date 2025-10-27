package org.example.chatapp.repository;

import org.example.chatapp.entity.User;
import org.example.chatapp.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.example.chatapp.entity.WorkspaceMember;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember,Integer> {
    Boolean existsByWorkspaceAndUser(Workspace workspace, User user);
    List<WorkspaceMember> findAllByUser_UserId(Integer userId);

    List<WorkspaceMember> findAllByWorkspace_Id(Integer workspaceId);
    Optional<WorkspaceMember> findByWorkspace_IdAndUser_UserId(Integer workspaceId, Integer userId);
}
