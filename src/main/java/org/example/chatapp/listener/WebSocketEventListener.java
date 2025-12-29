package org.example.chatapp.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatapp.repository.ConversationMemberRepository;
import org.example.chatapp.service.impl.WebSocketService;
import org.example.chatapp.ultis.PrincipalCast;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final WebSocketService webSocketService;
    private final ConversationMemberRepository conversationMemberRepository;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            try {
                Integer userId = PrincipalCast.castUserIdFromPrincipal(principal);
                log.info("User connected: {}", userId);
                broadcastUserStatus(userId, "ONLINE");
            } catch (Exception e) {
                log.error("Error handling connect event: {}", e.getMessage());
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            try {
                Integer userId = PrincipalCast.castUserIdFromPrincipal(principal);
                log.info("User disconnected: {}", userId);
                broadcastUserStatus(userId, "OFFLINE");
            } catch (Exception e) {
                log.error("Error handling disconnect event: {}", e.getMessage());
            }
        }
    }

    private void broadcastUserStatus(Integer userId, String status) {
        List<Integer> conversationIds = conversationMemberRepository.findConversationIdsByUserId(userId);
        
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("type", "USER_STATUS");
        statusUpdate.put("userId", userId);
        statusUpdate.put("status", status);

        for (Integer convId : conversationIds) {
            webSocketService.sendMessageToConversation(convId, statusUpdate);
        }
    }
}
