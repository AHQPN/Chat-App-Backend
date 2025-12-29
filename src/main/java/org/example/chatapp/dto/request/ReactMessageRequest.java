package org.example.chatapp.dto.request;

import lombok.Data;

@Data
public class ReactMessageRequest {
    private Integer messageId;
    private String emoji;
}
