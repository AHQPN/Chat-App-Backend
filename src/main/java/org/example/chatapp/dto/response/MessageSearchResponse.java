package org.example.chatapp.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageSearchResponse {
    private Integer id;
    private String content;
    private Long createdAt;
    private Integer senderId;
    private String senderName;
    private String senderAvatar;
}
