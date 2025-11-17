package org.example.chatapp.dto.response;

import lombok.Data;

@Data
public class PinMessageResponse {
    private Integer messageId;
    private Integer conversationId;
    private Integer pinnedById;
    private String pinnedByMember;
    private Long pinnedAt;
}
