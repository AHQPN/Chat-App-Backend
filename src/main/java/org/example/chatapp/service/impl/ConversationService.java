package org.example.chatapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.AddMembersRequest;
import org.example.chatapp.dto.request.CreateConversationRequest;
import org.example.chatapp.dto.request.UpdateConversationRequest;
import org.example.chatapp.entity.*;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.mapper.ConversationMapper;
import org.example.chatapp.repository.*;
import org.example.chatapp.service.enums.ConversationEnum;
import org.example.chatapp.service.enums.WorkspaceRoleEnum;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ConversationMapper conversationMapper;
    private final UserRepository userRepository;
    private final ConversationMemberRepository conversationMemberRepository;

    public void createConversation(CreateConversationRequest request,Integer creatorId){
        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_NOT_FOUND));

        WorkspaceMember creatorMember = workspaceMemberRepository.findByWorkspace_IdAndUser_UserId(request.getWorkspaceId(), creatorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_WORKSPACE));

        if (creatorMember.getRole() != WorkspaceRoleEnum.ADMIN) {
            throw new AppException(ErrorCode.NO_PERMISSION_IN_WORKSPACE);
        }

        if(ConversationEnum.DM == request.getType())
            request.setIsPrivate(true);

        Conversation conversation = conversationMapper.convertToConversation(request);
        conversation.setCreatedAt(System.currentTimeMillis());
        conversation.setCreator(creatorMember.getUser());
        conversationRepository.save(conversation);
    }

    public void updateConversation(UpdateConversationRequest request, Integer conversationId){
        Conversation conversation = conversationRepository.getConversationById(conversationId).orElseThrow(
                () -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND)
        );

        conversationMapper.toExistingCourse(conversation, request);


    }

    public Integer findMemberByUserId(Integer userId, Integer conversationId){
        ConversationMember member =  conversationMemberRepository.findByConversation_IdAndUser_UserId(conversationId,userId).orElseThrow();
        return member.getId();
    }

    public void addMemberToConversation(AddMembersRequest addMembersRequest, Integer conversationId){
        Conversation conversation = conversationRepository.getConversationById(conversationId).orElseThrow(
                () -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        Workspace workspace =  conversation.getWorkspace();
        Set<Integer> members = addMembersRequest.getMemberIds();
        List<User> users = userRepository.findAllById(members);

        List<ConversationMember> newMembers = users.stream()
                .filter(user -> workspaceMemberRepository.existsByWorkspaceAndUser(workspace, user))
                .filter(user -> !conversationMemberRepository.existsByConversationAndUser(conversation, user))
                .map(user -> {
                    ConversationMember cm = new ConversationMember();
                    cm.setConversation(conversation);
                    cm.setUser(user);
                    return cm;
                })
                .toList();

        if (!newMembers.isEmpty()) {
            conversationMemberRepository.saveAll(newMembers);
        }

    }

    public Boolean isMemberInConversation(Integer conversationId, Integer memberId){
        Conversation conversation = conversationRepository.getConversationById(conversationId).orElseThrow();
        User usr = userRepository.findById(memberId).orElseThrow();
        return conversationMemberRepository.existsByConversationAndUser(conversation,usr);

    }



}
