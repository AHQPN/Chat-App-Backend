package org.example.chatapp.repository;

import org.example.chatapp.entity.Workspace;
import org.example.chatapp.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace,Integer> {

}
