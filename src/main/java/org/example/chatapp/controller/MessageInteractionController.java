package org.example.chatapp.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.AttachFileRequest;
import org.example.chatapp.dto.request.PinRequest;
import org.example.chatapp.dto.request.ReactMessageRequest;
import org.example.chatapp.dto.response.MessageInteractionResponse;
import org.example.chatapp.entity.Attachment;
import org.example.chatapp.service.impl.MessageInteractionService;
import org.example.chatapp.service.impl.MessageService;
import org.example.chatapp.service.impl.WebSocketService;
import org.example.chatapp.ultis.PrincipalCast;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import org.example.chatapp.dto.response.ApiResponse;
import org.example.chatapp.dto.response.AttachmentResponse;
import org.example.chatapp.service.impl.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import org.example.chatapp.dto.request.TypingRequest;

@RestController
@RequestMapping("/msginteractions")
@RequiredArgsConstructor
public class MessageInteractionController {
    private final MessageInteractionService messageInteractionService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final MessageService messageService;
    private final WebSocketService webSocketService;
    private final FileService fileService;

    @MessageMapping("/conversation/typing")
    public void typingStatus(TypingRequest request, Principal principal) {
        Integer userId = PrincipalCast.castUserIdFromPrincipal(principal);
        messageInteractionService.sendTypingStatus(request.getConversationId(), request.getIsTyping(), userId);
    }

    @MessageMapping("/msg/react")
    public void reactMessage(ReactMessageRequest request, Principal principal) {
        Integer userId = PrincipalCast.castUserIdFromPrincipal(principal);
        MessageInteractionResponse updatedMessage = messageInteractionService.addReaction(request, request.getMessageId(), userId);
    }



    @MessageMapping("/msg/unreact")
    public void unreactMessage(ReactMessageRequest request, Principal principal) {
        Integer userId = PrincipalCast.castUserIdFromPrincipal(principal);
        messageInteractionService.removeReaction(request.getMessageId(), userId);
    }

    // Ghim message
    @MessageMapping("/msg/pin")
    public void pinMessage(PinRequest request, Principal principal) {
        Integer userId =PrincipalCast.castUserIdFromPrincipal(principal);
        MessageInteractionResponse pinnedMessage = messageInteractionService.pinMessage(request.getMessageId(), userId);
    }

    @MessageMapping("/msg/unpin")
    public void unpinMessage(PinRequest request, Principal principal) {
        Integer userId = PrincipalCast.castUserIdFromPrincipal(principal);
        messageInteractionService.unpinMessage(request.getMessageId());
    }
    // Đính kèm file
    @MessageMapping("/msg/attach")
    public void attachFile(AttachFileRequest request, Principal principal) throws IOException {
        Integer userId =PrincipalCast.castUserIdFromPrincipal(principal);
        Attachment messageWithFile = messageInteractionService.uploadAttachment(request);
        simpMessagingTemplate.convertAndSend("/topic/messages", messageWithFile);
    }

    @PostMapping("/attachments")
    public ResponseEntity<ApiResponse> uploadAttachments(@RequestParam("files") List<MultipartFile> files) {
        try {
            if (files.isEmpty()) {
                return ResponseEntity.ok().body(ApiResponse.builder().message("No files uploaded").build());
            }

            List<Integer> ids = fileService.uploadAttachments(files);

            return ResponseEntity.ok().body(ApiResponse.builder().data(ids).build());
        } catch (Exception e) {
            return ResponseEntity.ok().body(ApiResponse.builder().message("Error: " + e.getMessage()).build());
        }
    }

    @GetMapping("/pin-limit/{conversationId}")
    public ResponseEntity<ApiResponse> checkPinLimit(@PathVariable Integer conversationId) {
        boolean isLimitReached = messageInteractionService.checkPinLimit(conversationId);
        return ResponseEntity.ok(ApiResponse.builder().data(isLimitReached).build());
    }





}
