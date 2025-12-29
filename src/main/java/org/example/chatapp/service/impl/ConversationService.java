package org.example.chatapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.AddMembersRequest;
import org.example.chatapp.dto.request.CreateConversationRequest;
import org.example.chatapp.dto.request.SetRoleConversationMemberRequest;
import org.example.chatapp.dto.request.UpdateConversationRequest;
import org.example.chatapp.dto.response.ConversationResponse;
import org.example.chatapp.entity.Conversation;
import org.example.chatapp.entity.ConversationMember;
import org.example.chatapp.entity.User;
import org.example.chatapp.entity.Workspace;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.mapper.ConversationMapper;
import org.example.chatapp.repository.*;
import org.example.chatapp.dto.response.MemberSocketEvent;
import org.example.chatapp.security.model.UserDetailsImpl;
import org.example.chatapp.service.enums.ConversationEnum;
import org.example.chatapp.service.enums.ConversationRoleEnum;
import org.example.chatapp.service.enums.RoleEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ConversationMapper conversationMapper;
    private final UserRepository userRepository;
    private final ConversationMemberRepository conversationMemberRepository;

    private final WebSocketService webSocketService;

    @Transactional
    public void createConversation(CreateConversationRequest request, Integer creatorId){
        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_NOT_FOUND));

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra creator có trong workspace không
        if (!workspaceMemberRepository.existsByWorkspaceAndUser(workspace, creator)) {
            throw new AppException(ErrorCode.USER_NOT_IN_WORKSPACE);
        }

        // Xử lý theo loại conversation
        if (ConversationEnum.DM == request.getType()) {
            // DM: bắt buộc phải có đúng 1 người khác (tổng cộng 2 người bao gồm creator)
            if (request.getMemberIds() == null || request.getMemberIds().size() != 1) {
                throw new AppException(ErrorCode.CONVERSATION_REQUIRES_TWO_MEMBERS);
            }
            request.setIsPrivate(true);
        }

        // Tạo conversation
        Conversation conversation = conversationMapper.convertToConversation(request);
        conversation.setCreatedAt(System.currentTimeMillis());
        conversation.setCreator(creator);
        conversation.setWorkspace(workspace);
        conversationRepository.save(conversation);

        // Danh sách tất cả thành viên (creator + members)
        List<ConversationMember> allMembers = new ArrayList<>();

        // Thêm creator vào conversation
        ConversationMember creatorMember = new ConversationMember();
        creatorMember.setConversation(conversation);
        creatorMember.setUser(creator);
        creatorMember.setJoinedAt(System.currentTimeMillis());
        // DM không có ADMIN, CHANNEL thì creator là ADMIN
        creatorMember.setRole(ConversationEnum.DM == request.getType() ? ConversationRoleEnum.MEMBER : ConversationRoleEnum.ADMIN);
        allMembers.add(conversationMemberRepository.save(creatorMember));

        // Thêm các members khác nếu có
        if (request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            List<User> users = userRepository.findAllById(request.getMemberIds());
            
            List<ConversationMember> newMembers = users.stream()
                    .filter(user -> !user.getUserId().equals(creatorId)) // Loại trừ creator (đã thêm rồi)
                    .filter(user -> workspaceMemberRepository.existsByWorkspaceAndUser(workspace, user)) // Phải trong workspace
                    .map(user -> {
                        ConversationMember cm = new ConversationMember();
                        cm.setConversation(conversation);
                        cm.setUser(user);
                        cm.setJoinedAt(System.currentTimeMillis());
                        cm.setRole(ConversationRoleEnum.MEMBER); // Tất cả members khác đều là MEMBER
                        return cm;
                    })
                    .toList();
            
            if (!newMembers.isEmpty()) {
                allMembers.addAll(conversationMemberRepository.saveAll(newMembers));
            }
        }

        // Broadcast to all members
        List<ConversationResponse.MemberInfo> memberInfos = allMembers.stream()
                .map(m -> ConversationResponse.MemberInfo.builder()
                        .userId(m.getUser().getUserId())
                        .conversationMemberId(m.getId())
                        .fullName(m.getUser().getFullName())
                        .avatar(m.getUser().getAvatar())
                        .role(m.getRole() != null ? m.getRole().name() : "MEMBER")
                        .build())
                .toList();
                
        for (ConversationMember member : allMembers) {
            ConversationResponse response = ConversationResponse.builder()
                    .id(conversation.getId())
                    .name(conversation.getName())
                    .type(conversation.getType())
                    .isPrivate(conversation.getIsPrivate())
                    .createdAt(conversation.getCreatedAt())
                    .totalMembers(allMembers.size())
                    .members(memberInfos)
                    .isJoined(true)
                    .unseenCount(0)
                    .build();

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "NEW_CONVERSATION");
            payload.put("data", response);
            
            webSocketService.sendNotification(member.getUser().getUserId(), payload);
        }
    }

    public void updateConversation(UpdateConversationRequest request, Integer conversationId){
        Conversation conversation = conversationRepository.getConversationById(conversationId).orElseThrow(
                () -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND)
        );

        conversationMapper.toExistingCourse(conversation, request);


    }

    public ConversationMember findMemberByUserId(Integer userId, Integer conversationId){
        ConversationMember member =  conversationMemberRepository.findByConversation_IdAndUser_UserId(conversationId,userId).orElseThrow();
        return member;
    }

    @Transactional
    public void addMemberToConversation(AddMembersRequest addMembersRequest, Integer conversationId, UserDetailsImpl principal){
        Conversation conversation = conversationRepository.getConversationById(conversationId).orElseThrow(
                () -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        // Không cho phép thêm thành viên vào DM (DM chỉ có đúng 2 người)
        if (conversation.getType() == ConversationEnum.DM) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // Kiểm tra quyền: chỉ Admin hệ thống hoặc Admin của channel mới được thêm thành viên
        boolean isSystemAdmin = RoleEnum.Admin.name().equals(principal.getRole());
        
        if (!isSystemAdmin) {
            // Kiểm tra xem user có phải là admin của channel không
            ConversationMember requester = conversationMemberRepository
                    .findByConversation_IdAndUser_UserId(conversationId, principal.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CONVERSATION));
            
            if (requester.getRole() != ConversationRoleEnum.ADMIN) {
                throw new AppException(ErrorCode.FORBIDDEN);
            }
        }

        Workspace workspace =  conversation.getWorkspace();
        Set<Integer> members = addMembersRequest.getMemberIds();
        List<User> users = userRepository.findAllById(members);
        List<ConversationMember> membersToSave = new ArrayList<>();

        for (User user : users) {
             // 1. Check workspace membership
             if (!workspaceMemberRepository.existsByWorkspaceAndUser(workspace, user)) continue;

             // 2. Check conversation membership
            Optional<ConversationMember> existingMemberOpt = conversationMemberRepository.findByConversation_IdAndUser_UserId(conversationId, user.getUserId());
             
             if (existingMemberOpt.isPresent()) {
                 ConversationMember existingMember = existingMemberOpt.get();
                 if (existingMember.getRole() == org.example.chatapp.service.enums.ConversationRoleEnum.DELETED) {
                     // RESTORE
                     existingMember.setRole(ConversationRoleEnum.MEMBER);
                     existingMember.setJoinedAt(System.currentTimeMillis());
                     membersToSave.add(existingMember);
                 }
                 // If already MEMBER/ADMIN -> Do nothing (or duplicates ignored)
             } else {
                 // CREATE NEW
                 ConversationMember cm = new ConversationMember();
                 cm.setConversation(conversation);
                 cm.setUser(user);
                 cm.setRole(ConversationRoleEnum.MEMBER);
                 cm.setJoinedAt(System.currentTimeMillis());
                 cm.setIsNotifEnabled(true);
                 membersToSave.add(cm);
             }
        }

        if (!membersToSave.isEmpty()) {
            List<ConversationMember> savedMembers = conversationMemberRepository.saveAll(membersToSave);

            // Fetch all members to build correct response
            List<ConversationMember> allMembers = conversationMemberRepository.findAllByConversation_IdAndRoleNot(conversationId, ConversationRoleEnum.DELETED);
            
            List<ConversationResponse.MemberInfo> memberInfos = allMembers.stream()
                    .map(m -> ConversationResponse.MemberInfo.builder()
                            .userId(m.getUser().getUserId())
                            .conversationMemberId(m.getId())
                            .fullName(m.getUser().getFullName())
                            .avatar(m.getUser().getAvatar())
                            .role(m.getRole() != null ? m.getRole().name() : "MEMBER")
                            .build())
                    .toList();

            for (ConversationMember member : savedMembers) {
                // 1. Notify conversation (MEMBER_ADDED)
                MemberSocketEvent event = MemberSocketEvent.builder()
                        .type("MEMBER_ADDED")
                        .conversationId(conversationId)
                        .memberId(member.getId())
                        .userId(member.getUser().getUserId())
                        .fullName(member.getUser().getFullName())
                        .avatar(member.getUser().getAvatar())
                        .role(member.getRole())
                        .build();
                
                webSocketService.sendMessageToConversation(conversationId, event);

                // 2. Notify specific user (NEW_CONVERSATION)
                ConversationResponse response = ConversationResponse.builder()
                        .id(conversation.getId())
                        .name(conversation.getName())
                        .type(conversation.getType())
                        .isPrivate(conversation.getIsPrivate())
                        .createdAt(conversation.getCreatedAt())
                        .totalMembers(allMembers.size())
                        .members(memberInfos)
                        .isJoined(true)
                        .unseenCount(0)
                        .build();

                Map<String, Object> userPayload = new HashMap<>();
                userPayload.put("type", "NEW_CONVERSATION");
                userPayload.put("data", response);
                
                webSocketService.sendNotification(member.getUser().getUserId(), userPayload);
            }
        }

    }

    public Boolean isMemberInConversation(Integer conversationId, Integer memberId){
        Conversation conversation = conversationRepository.getConversationById(conversationId).orElseThrow();
        User usr = userRepository.findById(memberId).orElseThrow();
        return conversationMemberRepository.existsByConversationAndUser(conversation,usr);

    }



    public void setMemberRole(UserDetailsImpl principal, Integer conversationId, SetRoleConversationMemberRequest request) {
        boolean isAuthorized = false;

        if (Objects.equals(principal.getRole(), RoleEnum.Admin.name())) {
            isAuthorized = true;
        } else {
            ConversationMember requesterInfo = findMemberByUserId(principal.getId(), conversationId);
            if (requesterInfo.getRole() == ConversationRoleEnum.ADMIN) {
                isAuthorized = true;
            }
        }

        if (isAuthorized) {
            ConversationMember updatedMember = updateConversationMemberRole(conversationId, request.getConversationMemberId(), request.getConversationRole(), principal.getId());

            // WebSocket Notification
            MemberSocketEvent event = MemberSocketEvent.builder()
                    .type("MEMBER_ROLE_UPDATED")
                    .conversationId(conversationId)
                    .memberId(updatedMember.getId())
                    .userId(updatedMember.getUser().getUserId())
                    .role(updatedMember.getRole())
                    .build();

            webSocketService.sendMessageToConversation(conversationId, event);
        } else {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private ConversationMember updateConversationMemberRole(Integer conversationId, Integer memberId, ConversationRoleEnum newRole, Integer requesterId) {
        Conversation conversation = conversationRepository.getConversationById(conversationId).orElseThrow();
        ConversationMember member = conversationMemberRepository.getConversationMembersById(memberId);
        Boolean checked = isMemberInConversation(conversation.getId(), requesterId);
        if (Boolean.TRUE.equals(checked)) {
            member.setRole(newRole);
            return conversationMemberRepository.save(member);
        }
        throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversationsByUserId(Integer userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 1. Lấy tất cả workspace IDs của user
        List<Integer> workspaceIds = workspaceMemberRepository.findWorkspaceIdsByUserId(userId);

        if (workspaceIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. Lấy tất cả conversations trong workspace (joined + unjoined) - 1 query
        List<Conversation> allConversations = conversationRepository
                .findAllByWorkspaceIdIn(workspaceIds);

        // 3. Lấy tất cả conversation IDs
        List<Integer> conversationIds = allConversations.stream()
               .map(Conversation::getId)
                .collect(Collectors.toList());

        // 4. Lấy tất cả members của các conversations này - 1 query
        List<ConversationMember> allMembers = conversationMemberRepository
                .findAllByConversation_IdInAndRoleNot(conversationIds, ConversationRoleEnum.DELETED);

        // 5. Group members by conversation ID
        Map<Integer, List<ConversationMember>> membersByConversation = allMembers.stream()
                .collect(Collectors.groupingBy(m -> m.getConversation().getId()));

        // 6. Tìm conversation IDs mà user đã join
        Set<Integer> joinedConversationIds = allMembers.stream()
                .filter(m -> m.getUser().getUserId().equals(userId))
                .map(m -> m.getConversation().getId())
                .collect(Collectors.toSet());

        // 7. Build responses
        List<ConversationResponse> responses = new ArrayList<>();

        for (Conversation conversation : allConversations) {
            boolean isJoined = joinedConversationIds.contains(conversation.getId());

            // Skip private channels that user hasn't joined
            if (!isJoined && conversation.getIsPrivate()) {
                continue;
            }

            List<ConversationMember> conversationMembers =
                    membersByConversation.getOrDefault(conversation.getId(), new ArrayList<>());

            List<ConversationResponse.MemberInfo> memberInfos = null;

            if (conversation.getType() == ConversationEnum.DM && isJoined) {

                memberInfos = conversationMembers.stream()
                        .filter(m -> !m.getUser().getUserId().equals(userId))
                        .map(m -> ConversationResponse.MemberInfo.builder()
                                .userId(m.getUser().getUserId())
                                .conversationMemberId(m.getId())
                                .fullName(m.getUser().getFullName())
                                .avatar(m.getUser().getAvatar())
                                .role(m.getRole() != null ? m.getRole().name() : "MEMBER")
                                .build())
                        .collect(Collectors.toList());
            }
            Integer unseenCount  = 0;
            if(isJoined) {
                ConversationMember member = conversationMemberRepository.findByConversation_IdAndUser_UserId(conversation.getId(),userId).orElseThrow();

                if(member.getLastReadMessage() != null) {
                    System.out.println(member.getLastReadMessage().getContent());
                    unseenCount = messageRepository.countUnreadMessages(conversation.getId(),member.getLastReadMessage().getId(),userId);

                }
            }

            ConversationResponse res = ConversationResponse.builder()
                    .id(conversation.getId())
                    .name(conversation.getName())
                    .type(conversation.getType())
                    .unseenCount(unseenCount)
                    .isPrivate(conversation.getIsPrivate())
                    .createdAt(conversation.getCreatedAt())
                    .totalMembers(conversationMembers.size())
                    .members(memberInfos)
                    .isJoined(isJoined)
                    .build();

            responses.add(res);
        }

        return responses;
    }
    public ConversationResponse getConversationInfo(Integer conversationId, Integer userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!conversationMemberRepository.existsByConversationAndUser(conversation, user)) {
            throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
        }

        List<ConversationMember> members = conversationMemberRepository.findAllByConversation_IdAndRoleNot(conversationId, org.example.chatapp.service.enums.ConversationRoleEnum.DELETED);

        List<ConversationResponse.MemberInfo> memberInfos = members.stream()
                .map(m -> ConversationResponse.MemberInfo.builder()
                        .userId(m.getUser().getUserId())
                        .conversationMemberId(m.getId())
                        .fullName(m.getUser().getFullName())
                        .avatar(m.getUser().getAvatar())
                        .role(m.getRole() != null ? m.getRole().name() : "MEMBER")
                        .build())
                .toList();

        return ConversationResponse.builder()
                .id(conversation.getId())
                .name(conversation.getName())
                .type(conversation.getType())
                .isPrivate(conversation.getIsPrivate())
                .createdAt(conversation.getCreatedAt())
                .totalMembers(members.size())
                .members(memberInfos)
                .build();
    }

    @Transactional
    public void removeMembersFromConversation(Integer conversationId, org.example.chatapp.dto.request.RemoveMembersRequest request, Integer requesterId) {
        // 1. Check requester permissions
        ConversationMember requester = conversationMemberRepository
                .findByConversation_IdAndUser_UserId(conversationId, requesterId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CONVERSATION));

        if (requester.getRole() != org.example.chatapp.service.enums.ConversationRoleEnum.ADMIN) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // 2. Process removal
        for (Integer userIdToRemove : request.getUserIds()) {
            ConversationMember targetMember = conversationMemberRepository
                    .findByConversation_IdAndUser_UserId(conversationId, userIdToRemove)
                    .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));
            
            targetMember.setRole(org.example.chatapp.service.enums.ConversationRoleEnum.DELETED);
            conversationMemberRepository.save(targetMember);

            // WebSocket Notification
            MemberSocketEvent event = MemberSocketEvent.builder()
                    .type("MEMBER_REMOVED")
                    .conversationId(conversationId)
                    .userId(userIdToRemove)
                    .build();
            
            webSocketService.sendMessageToConversation(conversationId, event);
        }

    }

    public List<ConversationResponse> getPublicChannelsToJoin(Integer workspaceId, Integer userId) {
        // Find public channels that user hasn't joined yet
        List<Conversation> channels = conversationRepository.findPublicChannelsNotJoined(workspaceId, userId);

        return channels.stream()
                .map(c -> ConversationResponse.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .type(c.getType())
                        .isPrivate(c.getIsPrivate())
                        .createdAt(c.getCreatedAt())
                        .totalMembers(conversationMemberRepository.countByConversation_Id(c.getId()))
                        .build())
                .toList();
    }

    public void joinPublicChannel(Integer conversationId, Integer userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
        
        // 1. Check if Private
        if (Boolean.TRUE.equals(conversation.getIsPrivate())) {
            throw new AppException(ErrorCode.FORBIDDEN); 
        }
        
        // 2. Check workspace membership
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        if (!workspaceMemberRepository.existsByWorkspaceAndUser(conversation.getWorkspace(), user)) {
             throw new AppException(ErrorCode.USER_NOT_IN_WORKSPACE);
        }

        // 3. Check if already member
        java.util.Optional<ConversationMember> existingMemberOpt = conversationMemberRepository.findByConversation_IdAndUser_UserId(conversationId, userId);
        
        if (existingMemberOpt.isPresent()) {
            ConversationMember existing = existingMemberOpt.get();
            if (existing.getRole() == ConversationRoleEnum.DELETED) {
                // Restore
                existing.setRole(ConversationRoleEnum.MEMBER);
                existing.setJoinedAt(System.currentTimeMillis());
                conversationMemberRepository.save(existing);
                return;
            } else {
                throw new AppException(ErrorCode.USER_EXIST); 
            }
        }
        
        // 4. Join
        ConversationMember cm = new ConversationMember();
        cm.setConversation(conversation);
        cm.setUser(user);
        cm.setRole(ConversationRoleEnum.MEMBER);
        cm.setJoinedAt(System.currentTimeMillis());
        conversationMemberRepository.save(cm);
    }

    @Transactional
    public void setReadMessage(Integer conversationId, Integer messageId, Integer userId) {
        // 1. Verify user is member of conversation
        ConversationMember member = conversationMemberRepository
                .findByConversation_IdAndUser_UserId(conversationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CONVERSATION));

        // 2. Verify message exists and belongs to conversation
        org.example.chatapp.entity.Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new AppException(ErrorCode.MESSAGE_NOT_IN_CONVERSATION);
        }

        // 3. Update lastReadMessage and lastReadAt
        member.setLastReadMessage(message);
        member.setLastReadAt(System.currentTimeMillis());
        conversationMemberRepository.save(member);
    }
}
