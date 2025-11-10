package org.example.chatapp.dto.response;

import lombok.Data;

@Data
public class MessageResponse {
    private Integer id;

    private String content;
    private Boolean isDeleted;

    private Long createdAt;
    private Long updatedAt;

    private Integer conversationId;

    private Integer senderId;
    private String senderName;
    private String senderAvatar;

    // reply
    private Integer parentMessageId;
    private String parentContent;
}
