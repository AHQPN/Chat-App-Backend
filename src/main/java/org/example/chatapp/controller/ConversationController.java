package org.example.chatapp.controller;


import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.CreateConversationRequest;
import org.example.chatapp.dto.request.UpdateConversationRequest;
import org.example.chatapp.dto.request.AddMembersRequest;
import org.example.chatapp.security.model.UserDetailsImpl;
import org.example.chatapp.service.impl.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> createConversation(
            @RequestBody CreateConversationRequest request,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        conversationService.createConversation(request, principal.getId());
        return ResponseEntity.ok("Conversation created successfully");
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> addMembers(
            @PathVariable Integer conversationId,
            @RequestBody AddMembersRequest addMembersRequest) {

        conversationService.addMemberToConversation(addMembersRequest,conversationId);

        return ResponseEntity.ok("Members added successfully");
    }

}
