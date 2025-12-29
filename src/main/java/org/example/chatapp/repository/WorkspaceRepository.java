package org.example.chatapp.repository;

import org.example.chatapp.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace,Integer> {

    Workspace getWorkspaceById(int i);
}
