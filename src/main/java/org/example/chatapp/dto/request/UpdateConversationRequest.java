package org.example.chatapp.dto.request;

import lombok.Data;

@Data
public class UpdateConversationRequest {
    private String name;

    private Boolean isPrivate = false;
}
