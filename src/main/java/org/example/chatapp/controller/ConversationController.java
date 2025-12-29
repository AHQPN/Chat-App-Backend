package org.example.chatapp.controller;


import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.AddMembersRequest;
import org.example.chatapp.dto.request.CreateConversationRequest;
import org.example.chatapp.dto.request.SetRoleConversationMemberRequest;
import org.example.chatapp.dto.request.UpdateConversationRequest;
import org.example.chatapp.dto.response.ApiResponse;
import org.example.chatapp.dto.response.ConversationResponse;
import org.example.chatapp.security.model.UserDetailsImpl;
import org.example.chatapp.service.impl.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<String> createConversation(
            @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        conversationService.createConversation(request, principal.getId());
        return ResponseEntity.ok("Conversation created successfully");
    }
    @PostMapping("/{conversationId}")
    public ResponseEntity<?> setRole(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PathVariable Integer conversationId,
            @RequestBody SetRoleConversationMemberRequest request
            ){
        conversationService.setMemberRole(principal,conversationId,request);
        return ResponseEntity.ok("Member role set successfully");
    }

    @PutMapping("/{conversationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateConversation(
            @PathVariable Integer conversationId,
            @RequestBody UpdateConversationRequest request) {

        conversationService.updateConversation(request, conversationId);
        return ResponseEntity.ok("Conversation updated successfully");
    }

    @PostMapping("/{conversationId}/members")
    public ResponseEntity<ApiResponse> addMembers(
            @PathVariable Integer conversationId,
            @RequestBody AddMembersRequest addMembersRequest,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        conversationService.addMemberToConversation(addMembersRequest, conversationId, principal);

        return ResponseEntity.ok().body(ApiResponse.builder().message("Successfully added users to conversation").build());
    }

    @PostMapping("/read")
    public ResponseEntity<ApiResponse> setReadMessage(
            @RequestBody org.example.chatapp.dto.request.SetReadMessageRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        
        conversationService.setReadMessage(request.getConversationId(), request.getMessageId(), principal.getId());
        return ResponseEntity.ok().body(ApiResponse.builder().message("Read status updated successfully").build());
    }

    @GetMapping("/user/me")
    public ResponseEntity<ApiResponse> getMyConversations(
            @AuthenticationPrincipal UserDetailsImpl principal) {
        List<ConversationResponse> conversations = conversationService.getConversationsByUserId(principal.getId());
        return ResponseEntity.ok().body(ApiResponse.builder().data(conversations).build());
    }
    @GetMapping("/{conversationId}")
    public ResponseEntity<ApiResponse> getConversationInfo(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PathVariable Integer conversationId
            )
    {
        ConversationResponse conversationResponse = conversationService.getConversationInfo(conversationId, principal.getId());
        return ResponseEntity.ok().body(ApiResponse.builder().data(conversationResponse).build());
    }

    @DeleteMapping("/{conversationId}/members")
    public ResponseEntity<ApiResponse> removeMembers(
            @PathVariable Integer conversationId,
            @RequestBody org.example.chatapp.dto.request.RemoveMembersRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        
        conversationService.removeMembersFromConversation(conversationId, request, principal.getId());
        return ResponseEntity.ok().body(ApiResponse.builder().message("Members removed successfully").build());
    }

    @GetMapping("/workspace/{workspaceId}/public")
    public ResponseEntity<ApiResponse> getPublicChannels(
            @PathVariable Integer workspaceId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        List<ConversationResponse> channels = conversationService.getPublicChannelsToJoin(workspaceId, userDetails.getId());
        return ResponseEntity.ok().body(ApiResponse.builder().data(channels).build());
    }

    @PostMapping("/{conversationId}/join")
    public ResponseEntity<ApiResponse> joinChannel(
            @PathVariable Integer conversationId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        conversationService.joinPublicChannel(conversationId, userDetails.getId());
        return ResponseEntity.ok().body(ApiResponse.builder().message("Joined successfully").build());
    }

}
