package org.example.chatapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.example.chatapp.service.enums.MessageStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Integer id;

    private String content;
    private MessageStatus status;

    private Long createdAt;
    private Long updatedAt;

    private Integer conversationId;

    private Integer senderId;
    private String senderName;
    private String senderAvatar;

    // reply
    private Integer parentMessageId;
    private String parentContent;

    // interactions
    private List<ReactionInfo> reactions;
    private List<MentionInfo> mentions;
    private Boolean isPinned;
    private List<AttachmentInfo> attachments;
    private Integer threadId;
    private Integer threadReplyCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactionInfo {
        private Integer userId;
        private String userName;
        private String emoji;
        private Long reactedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MentionInfo {
        private Integer memberId;  // conversationMemberId
        private Integer userId;
        private String userName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentInfo {
        private Integer id;
        private String fileUrl;
        private String fileType;
        private Long fileSize;
    }
}
