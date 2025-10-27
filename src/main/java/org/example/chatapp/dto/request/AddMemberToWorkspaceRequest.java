package org.example.chatapp.dto.request;

import lombok.Data;
import org.example.chatapp.service.enums.WorkspaceRoleEnum;

@Data
public class AddMemberToWorkspaceRequest {
    private Integer workspaceId, newMemberId;
    private WorkspaceRoleEnum role;
}
