package org.example.chatapp.controller;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.CreateMessageRequest;
import org.example.chatapp.dto.request.MessageUpdateRequest;
import org.example.chatapp.dto.response.ApiResponse;
import org.example.chatapp.dto.response.MessageResponse;
import org.example.chatapp.dto.response.MessageSearchResponse;
import org.example.chatapp.entity.ConversationMember;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.security.model.UserDetailsImpl;
import org.example.chatapp.service.impl.ConversationService;
import org.example.chatapp.service.impl.MessageService;
import org.example.chatapp.ultis.PrincipalCast;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
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
            Principal principal) {

        if (principal == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // Cast để lấy UserDetailsImpl

        Integer userId =PrincipalCast.castUserIdFromPrincipal(principal);

        ConversationMember conversationMember = conversationService.findMemberByUserId(userId, conversationId);
        messageService.createMessage(request, conversationMember.getId(),conversationId, request.getParentMessageId());
    }
    @MessageMapping()


    //  Lấy danh sách tin nhắn
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<?> getMessages(@PathVariable Integer conversationId,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "15") int size,
                                         @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        if(!conversationService.isMemberInConversation(conversationId,userDetails.getId()))
            throw new AppException(ErrorCode.ACCESS_DENIED);

        Page<MessageResponse> messageResponses = messageService.getLatestMessages(conversationId, userDetails.getId(), page, size);
        return ResponseEntity.ok().body(ApiResponse.builder().data(messageResponses).build());
    }

    /**
     * Lấy page chứa tin nhắn cụ thể (để navigate đến tin nhắn reply gốc).
     * Response giống hệt endpoint lấy danh sách tin nhắn, nhưng page được tính toán tự động.
     */
    @GetMapping("/{messageId}/context")
    public ResponseEntity<?> getMessageContext(
            @PathVariable Integer messageId,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Page<MessageResponse> messageResponses = messageService.getMessageContext(messageId, userDetails.getId(), size);
        return ResponseEntity.ok().body(ApiResponse.builder().data(messageResponses).build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateMessage(@PathVariable Integer id, @RequestBody MessageUpdateRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        messageService.updateMessage(id,userDetails.getId(),request);
        return  ResponseEntity.ok().body(ApiResponse.builder().message("Cap nhat thanh cong").build());
    }

    /**
     * Thu hồi tin nhắn với mọi người
     * Chỉ người gửi mới có quyền thu hồi tin nhắn của mình
     */
    @DeleteMapping("/{messageId}/revoke")
    public ResponseEntity<?> revokeMessage(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        messageService.revokeMessage(messageId, userDetails.getId());
        return ResponseEntity.ok().body(ApiResponse.builder()
                .message("Tin nhắn đã được thu hồi")
                .build());
    }

    /**
     * Xóa tin nhắn ở phía tôi (chỉ người gửi không thấy, người khác vẫn thấy)
     */
    @DeleteMapping("/{messageId}/delete-for-me")
    public ResponseEntity<?> deleteMessageForMe(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        messageService.deleteMessageForMe(messageId, userDetails.getId());
        return ResponseEntity.ok().body(ApiResponse.builder()
                .message("Tin nhắn đã được xóa ở phía bạn")
                .build());
    }

    @GetMapping("/{messageId}/thread")
    public ResponseEntity<ApiResponse> getThreadMessages(
            @PathVariable Integer messageId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<MessageResponse> messages = messageService.getMessagesInThread(messageId, userDetails.getId(), page, size);
        return ResponseEntity.ok().body(ApiResponse.builder().data(messages).build());
    }

    /**
     * Tìm kiếm tin nhắn theo từ khóa trong conversation
     */
    @GetMapping("/conversation/{conversationId}/search")
    public ResponseEntity<ApiResponse> searchMessages(
            @PathVariable Integer conversationId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        if (!conversationService.isMemberInConversation(conversationId, userDetails.getId())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        Page<MessageSearchResponse> messages = messageService.searchMessages(
                conversationId, userDetails.getId(), keyword, page, size);
        return ResponseEntity.ok().body(ApiResponse.builder().data(messages).build());
    }

}
