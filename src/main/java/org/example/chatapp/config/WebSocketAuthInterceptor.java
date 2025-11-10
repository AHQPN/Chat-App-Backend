package org.example.chatapp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.repository.ConversationMemberRepository;
import org.example.chatapp.security.jwt.JwtUtils;
import org.example.chatapp.security.model.UserDetailsImpl;
import org.example.chatapp.security.services.UserDetailsServiceImpl;
import org.example.chatapp.service.impl.ConversationService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final ConversationService conversationService;
    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;
    private final ConversationMemberRepository conversationMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        log.debug("Processing {} command to {}", accessor.getCommand(), accessor.getDestination());

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        }
        else {
            restoreAuthenticationFromSession(accessor);
        }

        if (!isAuthenticated(accessor)) {
            if (requiresAuthentication(accessor.getCommand())) {
                log.warn("Blocked unauthenticated {} request to {}",
                        accessor.getCommand(), accessor.getDestination());
                return null;
            }
            return message;
        }

        Integer userId = extractUserId(accessor);
        if (userId == null) {
            log.error("Cannot extract userId from authenticated user");
            return null;
        }

        log.debug("User {} executing {} to {}", userId, accessor.getCommand(), accessor.getDestination());

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (!authorizeSubscribe(accessor, userId)) {
                return null;
            }
        }

        if (StompCommand.SEND.equals(accessor.getCommand())) {
            if (!authorizeSend(accessor, userId)) {
                return null;
            }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");

        if (authHeaders == null || authHeaders.isEmpty()) {
            log.warn("Missing Authorization header in CONNECT");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String fullTokenHeader = authHeaders.get(0);

        if (fullTokenHeader == null || !fullTokenHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header format");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String token = fullTokenHeader.substring(7);

        try {
            if (!jwtUtils.validateJwtToken(token)) {
                throw new AppException(ErrorCode.UNAUTHENTICATED);
            }

            String identifier = jwtUtils.getIdentifierFromJwtToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(identifier);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());

            accessor.setUser(authentication);

            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put("AUTHENTICATED_USER", authentication);
                log.info("User {} authenticated via WebSocket",
                        ((UserDetailsImpl) userDetails).getId());
            }

        } catch (Exception e) {
            log.error("JWT Authentication Failed for WebSocket: {}", e.getMessage());
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private void restoreAuthenticationFromSession(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null && accessor.getSessionAttributes() != null) {
            Object storedAuth = accessor.getSessionAttributes().get("AUTHENTICATED_USER");
            if (storedAuth instanceof UsernamePasswordAuthenticationToken) {
                accessor.setUser((UsernamePasswordAuthenticationToken) storedAuth);
                log.debug("Restored authentication from session");
            }
        }
    }

    private boolean isAuthenticated(StompHeaderAccessor accessor) {
        return accessor.getUser() != null && accessor.getUser().getName() != null;
    }

    private boolean requiresAuthentication(StompCommand command) {
        return StompCommand.SUBSCRIBE.equals(command) ||
                StompCommand.SEND.equals(command) ||
                StompCommand.MESSAGE.equals(command);
    }

    private Integer extractUserId(StompHeaderAccessor accessor) {
        Object principal = accessor.getUser();

        if (!(principal instanceof UsernamePasswordAuthenticationToken)) {
            log.error("Principal is not UsernamePasswordAuthenticationToken: {}",
                    principal.getClass());
            return null;
        }

        Object userDetails = ((UsernamePasswordAuthenticationToken) principal).getPrincipal();

        if (!(userDetails instanceof UserDetailsImpl)) {
            log.error("UserDetails is not UserDetailsImpl: {}", userDetails.getClass());
            return null;
        }

        return ((UserDetailsImpl) userDetails).getId();
    }

    private boolean authorizeSubscribe(StompHeaderAccessor accessor, Integer userId) {
        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith("/topic/conversation/")) {
            return true;
        }

        try {
            String convIdStr = destination.substring("/topic/conversation/".length());
            Integer convId = Integer.parseInt(convIdStr);

            if (!conversationService.isMemberInConversation(convId, userId)) {
                log.warn("User {} is not a member of conversation {}", userId, convId);
                return false;
            }

            log.debug("User {} authorized to subscribe to conversation {}", userId, convId);
            return true;

        } catch (NumberFormatException e) {
            log.error("Invalid conversation ID in destination: {}", destination);
            return false;
        }
    }

    private boolean authorizeSend(StompHeaderAccessor accessor, Integer userId) {
        String destination = accessor.getDestination();

        if (destination == null || !destination.startsWith("/app/message.send/")) {
            return true;
        }

        try {
            String convIdStr = destination.substring("/app/message.send/".length());
            Integer convId = Integer.parseInt(convIdStr);

            if (!conversationService.isMemberInConversation(convId, userId)) {
                log.warn("User {} cannot send to conversation {}", userId, convId);
                return false;
            }

            log.debug("User {} authorized to send to conversation {}", userId, convId);
            return true;

        } catch (NumberFormatException e) {
            log.error("Invalid conversation ID in destination: {}", destination);
            return false;
        }
    }
}