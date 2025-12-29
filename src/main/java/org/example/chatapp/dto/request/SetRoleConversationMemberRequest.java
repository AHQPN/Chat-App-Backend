package org.example.chatapp.dto.request;

import lombok.Data;
import org.example.chatapp.service.enums.ConversationRoleEnum;

@Data
public class SetRoleConversationMemberRequest {
    Integer conversationMemberId;
    ConversationRoleEnum conversationRole;
}
