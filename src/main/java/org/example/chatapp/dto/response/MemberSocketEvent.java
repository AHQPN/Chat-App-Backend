package org.example.chatapp.dto.response;

import lombok.Builder;
import lombok.Data;
import org.example.chatapp.service.enums.ConversationRoleEnum;

@Data
@Builder
public class MemberSocketEvent {
    private String type;
    private Integer conversationId;
    private Integer memberId;
    private Integer userId;
    private String fullName;
    private String avatar;
    private ConversationRoleEnum role;
}
