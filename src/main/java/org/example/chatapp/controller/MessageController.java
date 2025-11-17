package org.example.chatapp.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.CreateMessageRequest;
import org.example.chatapp.dto.request.MessageUpdateRequest;
import org.example.chatapp.dto.response.ApiResponse;
import org.example.chatapp.dto.response.MessageResponse;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.security.model.UserDetailsImpl;
import org.example.chatapp.service.impl.ConversationService;
import org.example.chatapp.service.impl.MessageService;
import org.example.chatapp.ultis.PrincipalCast;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationService conversationService;
//    private final MessageInteractionService messageInteractionService;

    @MessageMapping("/message.send/{conversationId}")
    public void sendMessage(
            @Payload CreateMessageRequest request,
            @DestinationVariable Integer conversationId,
            Principal principal,
            @Header(value = "parentMessageId", required = false) Integer parentMessageId) {

        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Cast để lấy UserDetailsImpl

        Integer userId =PrincipalCast.castUserIdFromPrincipal(principal);

        Integer conversationMemberId = conversationService.findMemberByUserId(userId, conversationId);
        messageService.createMessage(request, conversationMemberId,conversationId,parentMessageId);
    }


    //  Lấy danh sách tin nhắn
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getMessages(@PathVariable Integer conversationId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "15") int size,
                                         @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        if(!conversationService.isMemberInConversation(conversationId,userDetails.getId()))
            throw new AppException(ErrorCode.ACCESS_DENIED);

        Page<MessageResponse> messageResponses =  messageService.getLatestMessages(conversationId,page,size);
        return ResponseEntity.ok().body(ApiResponse.builder().data(messageResponses).build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateMessage(@PathVariable Integer id, @RequestBody MessageUpdateRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        messageService.updateMessage(id,userDetails.getId(),request);
        return  ResponseEntity.ok().body(ApiResponse.builder().message("Cap nhat thanh cong").build());
    }

    @DeleteMapping
    public ResponseEntity<?> deleteMessage(@AuthenticationPrincipal UserDetailsImpl userDetails, @PathVariable Integer messageId)
    {
        messageService.deleteMessage(messageId, userDetails.getId());
        return ResponseEntity.ok().body(ApiResponse.builder().message("Tin nhan da xoa"));
    }

}
