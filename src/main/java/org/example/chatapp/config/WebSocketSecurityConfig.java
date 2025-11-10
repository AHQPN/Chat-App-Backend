package org.example.chatapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    public void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
                // User phải login mới gửi message lên /app/**
                .simpMessageDestMatchers("/app/**").authenticated()
                // Chỉ ADMIN mới subscribe topic admin
                .simpSubscribeDestMatchers("/topic/admin/**").hasRole("ADMIN")
                // Các topic khác login mới sub được
                .simpSubscribeDestMatchers("/topic/**").authenticated()
                .anyMessage().denyAll();
    }
}
