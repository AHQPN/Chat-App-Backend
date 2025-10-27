package org.example.chatapp.service.impl;

import lombok.AllArgsConstructor;
import org.example.chatapp.dto.request.AddMemberToWorkspaceRequest;
import org.example.chatapp.dto.request.WorkspaceRequest;
import org.example.chatapp.entity.User;
import org.example.chatapp.entity.Workspace;
import org.example.chatapp.entity.WorkspaceMember;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.repository.UserRepository;
import org.example.chatapp.repository.WorkspaceMemberRepository;
import org.example.chatapp.repository.WorkspaceRepository;
import org.example.chatapp.service.enums.WorkspaceRoleEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;




    @Transactional
    public Workspace createNewWorkspace(WorkspaceRequest workspace,Integer createrId) {
        User creator = userRepository.findById(createrId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Workspace newWorkspace = new Workspace();
        newWorkspace.setName(workspace.getName());
        newWorkspace.setCreator(creator);
        newWorkspace.setCreatedAt(System.currentTimeMillis());

        Workspace savedWorkspace = workspaceRepository.save(newWorkspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(savedWorkspace);
        member.setUser(creator);
        member.setRole(WorkspaceRoleEnum.ADMIN);
        member.setJoinedAt(System.currentTimeMillis());

        memberRepository.save(member);

        return savedWorkspace;
    }

    @Transactional
    public WorkspaceMember addMemberToWorkspace(AddMemberToWorkspaceRequest request,Integer createrId) {

        WorkspaceMember creatorMember = memberRepository.findByWorkspace_IdAndUser_UserId(request.getWorkspaceId(), createrId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_WORKSPACE));

        if (creatorMember.getRole() != WorkspaceRoleEnum.ADMIN) {
            throw new AppException(ErrorCode.NO_PERMISSION_IN_WORKSPACE);
        }

        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_NOT_FOUND));

        User newMember = userRepository.findById(request.getNewMemberId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (memberRepository.existsByWorkspaceAndUser(workspace, newMember)) {
            throw new AppException(ErrorCode.MEMBER_ALREADY_IN_WORKSPACE);
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(newMember);
        WorkspaceRoleEnum role = request.getRole();
        member.setRole(role != null ? role: WorkspaceRoleEnum.MEMBER);
        member.setJoinedAt(System.currentTimeMillis());

        return memberRepository.save(member);
    }


    public List<Workspace> getWorkspacesByUser(Integer userId) {
        return memberRepository.findAllByUser_UserId(userId).stream()
                .map(WorkspaceMember::getWorkspace)
                .toList();
    }

    public List<WorkspaceMember> getMembersOfWorkspace(Integer workspaceId) {
        return memberRepository.findAllByWorkspace_Id(workspaceId);
    }

    @Transactional
    public void deleteWorkspace(Integer workspaceId) {
        memberRepository.deleteById(workspaceId);
    }



}
