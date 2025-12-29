package org.example.chatapp.config;

import lombok.extern.slf4j.Slf4j;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.security.jwt.JwtUtils;
import org.example.chatapp.security.model.UserDetailsImpl;
import org.example.chatapp.security.services.UserDetailsServiceImpl;
import org.example.chatapp.service.impl.ConversationService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component

public class ConversationMembershipInterceptor implements ChannelInterceptor {

    private final ConversationService conversationService;
    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    public ConversationMembershipInterceptor(
            @org.springframework.context.annotation.Lazy ConversationService conversationService,
            JwtUtils jwtUtils,
            UserDetailsServiceImpl userDetailsService) {
        this.conversationService = conversationService;
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        assert accessor != null;
        StompCommand command = accessor.getCommand();

        log.debug("Processing {} to {}", command, accessor.getDestination());


        if (StompCommand.CONNECT.equals(command)) {
            return handleConnect(accessor, message);
        }


        restoreAuthenticationFromSession(accessor);

        if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
            return handleAuthorizationCheck(accessor, message);
        }
        accessor.setUser(accessor.getUser());
        return message;
    }

    private Message<?> handleConnect(StompHeaderAccessor accessor, Message<?> message) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");

        if (authHeaders == null || authHeaders.isEmpty()) {
            log.warn("Missing Authorization header in CONNECT");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String token = authHeaders.get(0).replace("Bearer ", "");

        try {
            if (!jwtUtils.validateJwtToken(token)) {
                log.warn("Invalid JWT token");
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String identifier = jwtUtils.getIdentifierFromJwtToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);

            UsernamePasswordAuthenticationToken authentication =
                    new WebSocketAuthentication(
                            userDetails,
                            null,
                            userDetails.getAuthorities());

            //  Set user cho CONNECT frame
            SecurityContextHolder.getContext().setAuthentication(authentication);
            accessor.setUser(authentication);

            //   Lưu vào session để dùng cho các message sau
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("AUTHENTICATED_USER", authentication);
                log.info(" User {} authenticated via WebSocket",
                        ((UserDetailsImpl) userDetails).getId());
            } else {
                log.warn("Session attributes is null - authentication won't persist!");
            }

        } catch (Exception e) {
            log.error("Authentication failed: {}", e.getMessage());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return message;
    }

    // Custom Authentication để getName() trả về userId thay vì username/phone
    // Giúp WebSocketService.convertAndSendToUser(userId, ...) hoạt động đúng
    private static class WebSocketAuthentication extends UsernamePasswordAuthenticationToken {
        public WebSocketAuthentication(Object principal, Object credentials, java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities) {
            super(principal, credentials, authorities);
        }

        @Override
        public String getName() {
            if (getPrincipal() instanceof UserDetailsImpl) {
                return String.valueOf(((UserDetailsImpl) getPrincipal()).getId());
            }
            return super.getName();
        }
    }

    private void restoreAuthenticationFromSession(StompHeaderAccessor accessor) {
        // Nếu chưa có user, lấy từ session
        if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
            Object storedAuth = accessor.getSessionAttributes().get("AUTHENTICATED_USER");

            if (storedAuth instanceof UsernamePasswordAuthenticationToken auth) {
                accessor.setUser(auth);
                log.debug(" Restored authentication for user: {}", auth.getName());
            } else {
                log.warn(" No authenticated user found in session");
            }
        }
    }

    private Message<?> handleAuthorizationCheck(StompHeaderAccessor accessor, Message<?> message) {
        Object principal = accessor.getUser();

        // Check authentication
        if (!(principal instanceof UsernamePasswordAuthenticationToken auth)) {
            log.warn(" Unauthenticated {} to {}",
                    accessor.getCommand(), accessor.getDestination());
            return null; // Block message
        }

        Integer userId = ((UserDetailsImpl) auth.getPrincipal()).getId();
        String destination = accessor.getDestination();

        // Check authorization for conversation topics
        if (destination != null && destination.startsWith("/topic/conversation/")) {
            try {
                Integer convId = Integer.parseInt(
                        destination.substring("/topic/conversation/".length()));

                if (!conversationService.isMemberInConversation(convId, userId)) {
                    log.warn("❌ User {} not member of conversation {}", userId, convId);
                    return null;
                }

                log.debug("✅ User {} authorized for conversation {}", userId, convId);
            } catch (NumberFormatException e) {
                log.error("Invalid conversation ID in destination: {}", destination);
                return null;
            }
        }

        // Check authorization for sending messages
        if (destination != null && destination.startsWith("/app/message.send/")) {
            try {
                Integer convId = Integer.parseInt(
                        destination.substring("/app/message.send/".length()));

                if (!conversationService.isMemberInConversation(convId, userId)) {
                    log.warn("❌ User {} cannot send to conversation {}", userId, convId);
                    return null;
                }

                log.debug("✅ User {} authorized to send to conversation {}", userId, convId);
            } catch (NumberFormatException e) {
                log.error("Invalid conversation ID in destination: {}", destination);
                return null;
            }
        }
        UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) accessor.getUser();
        SecurityContextHolder.getContext().setAuthentication(authentication);
        accessor.setUser(authentication);

        return message;
    }
}