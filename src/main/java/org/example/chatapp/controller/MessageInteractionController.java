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

@RestController
@RequestMapping("/msginteractions")
@RequiredArgsConstructor
public class MessageInteractionController {
    private final MessageInteractionService messageInteractionService;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final MessageService messageService;
    private final WebSocketService webSocketService;

    @MessageMapping("/msg/react/{messageId}")
    public void reactMessage(ReactMessageRequest request, Principal principal, @PathVariable Integer messageId) {
        Integer userId = PrincipalCast.castUserIdFromPrincipal(principal);
        MessageInteractionResponse updatedMessage = messageInteractionService.addReaction(request ,messageId,userId );
    }

    public void removeReaction(ReactMessageRequest request, Principal principal) {

    }

    @MessageMapping("/msg/unreact/{messageId}")
    public void unreactMessage(ReactMessageRequest request, Principal principal, @PathVariable Integer messageId) {
        Integer userId = PrincipalCast.castUserIdFromPrincipal(principal);
        messageInteractionService.removeReaction(messageId,userId);
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

    @MessageMapping("/msg/mention")
    public void mentionMember(Principal principal, List<Integer> memberIds) {
        Integer userId = PrincipalCast.castUserIdFromPrincipal(principal);
        messageInteractionService.mention(userId, memberIds);
    }

    // Đính kèm file
    @MessageMapping("/msg/attach")
    public void attachFile(AttachFileRequest request, Principal principal) throws IOException {
        Integer userId =PrincipalCast.castUserIdFromPrincipal(principal);
        Attachment messageWithFile = messageInteractionService.uploadAttachment(request);
        simpMessagingTemplate.convertAndSend("/topic/messages", messageWithFile);
    }





}
