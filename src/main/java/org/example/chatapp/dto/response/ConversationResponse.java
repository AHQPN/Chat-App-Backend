package org.example.chatapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.chatapp.service.enums.ConversationEnum;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConversationResponse {
    private Integer id;
    private String name;
    private ConversationEnum type;
    private Boolean isPrivate;
    private Long createdAt;
    private Boolean isJoined;
    private Integer unseenCount;

    private Integer totalMembers;
    private java.util.List<MemberInfo> members;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MemberInfo {
        private Integer userId;
        private Integer conversationMemberId;
        private String fullName;
        private String avatar;
        private String role;
    }
}
