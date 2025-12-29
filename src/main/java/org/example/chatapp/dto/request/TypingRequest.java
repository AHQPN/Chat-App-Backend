package org.example.chatapp.dto.request;

import lombok.Data;

@Data
public class TypingRequest {
    private Integer conversationId;
    private Boolean isTyping;
}
