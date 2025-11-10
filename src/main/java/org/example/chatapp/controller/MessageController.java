package org.example.chatapp.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.CreateMessageRequest;
import org.example.chatapp.dto.request.MessageUpdateRequest;
import org.example.chatapp.dto.request.ReactMessageRequest;
import org.example.chatapp.dto.response.ApiResponse;
import org.example.chatapp.dto.response.MessageResponse;
import org.example.chatapp.entity.ConversationMember;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.security.model.UserDetailsImpl;
import org.example.chatapp.service.impl.ConversationService;
import org.example.chatapp.service.impl.MessageInteractionService;
import org.example.chatapp.service.impl.MessageService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private final MessageInteractionService messageInteractionService;
    @MessageMapping("/message.send/{conversationId}")
    public void sendMessage(
            @Payload CreateMessageRequest request,
            @DestinationVariable Integer conversationId,
            SimpMessageHeaderAccessor headerAccessor,
            @Header(value = "parentMessageId", required = false) Integer parentMessageId) {
        Principal principal = headerAccessor.getUser();

        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        Integer userId = userDetails.getId();

        ConversationMember conversationMember = conversationService.findMemberByUserId(userId, conversationId);
        MessageResponse messageResponse = messageService.createMessage(request, conversationMember.getId(),conversationId,parentMessageId);
    }

    @MessageMapping("/message/{messageId}")
    public void reactMessage(
            @Payload ReactMessageRequest request,
            @DestinationVariable Integer messageId
            )
    {
        messageInteractionService.addReaction(messageId,1,request.getEmoji());
    }




//    @MessageMapping("/messages/react")
//    public void react(@Payload ReactionRequest request) {
//        messageInteractionService.addReaction(request.getMessageId(), request.getMemberId(), request.getEmoji());
//
//        // 2️⃣ Push realtime tới tất cả client subscribe conversation
//        messagingTemplate.convertAndSend(
//                "/topic/conversation." + request.getConversationId(),
//                new ReactionResponse(request.getMessageId(), request.getMemberId(), request.getEmoji())
//        );
//    }


    //  Lấy danh sách tin nhắn
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getMessages(@PathVariable Integer conversationId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "15") int size,
                                         @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        if(!conversationService.isMemberInConversation(conversationId,userDetails.getId()))
            throw new AppException(ErrorCode.ACCESS_DENIED);

        Page<MessageResponse> messageResponses =  messageService.getMessagesByConversation(conversationId,page,size);
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
