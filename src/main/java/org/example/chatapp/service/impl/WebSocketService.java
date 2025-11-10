package org.example.chatapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.repository.MessageRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final MessageRepository messageRepository;
    /**
     *  Gửi tin nhắn đến 1 conversation room
     * Client sẽ subscribe:  /topic/conversation/{conversationId}
     */
    // Gửi cho tất cả client subscribe room: /topic/conversation/{id}
    public void sendMessageToConversation(Integer conversationId, Object payload) {
        simpMessagingTemplate.convertAndSend(
                "/topic/conversation/" + conversationId,
                payload
        );
    }

//    // ✅ Gửi riêng cho một user
//    public void sendPrivate(Integer userId, Object payload) {
//        simpMessagingTemplate.convertAndSendToUser(
//                String.valueOf(userId),
//                "/queue/messages",
//                payload
//        );
//    }

    /**
     *  Gửi broadcast đến toàn bộ user
     * Client sẽ subscribe: /topic/public
     */
    public void broadcast(Object payload) {
        simpMessagingTemplate.convertAndSend("/topic/public", payload);
    }

    /**
     * Ví dụ gửi notification riêng
     * Client subscribe: /user/queue/notifications
     */
    public void sendNotification(Integer userId, Object payload) {
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(userId),
                "/queue/notifications",
                payload
        );
    }
}
