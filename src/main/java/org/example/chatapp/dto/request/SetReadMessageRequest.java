package org.example.chatapp.dto.request;

import lombok.Data;

@Data
public class SetReadMessageRequest {
    private Integer conversationId;
    private Integer messageId;
}
