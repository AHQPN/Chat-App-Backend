package org.example.chatapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.chatapp.service.enums.ConversationEnum;

import java.util.Set;
@Data
public class CreateConversationRequest {
    @NotBlank
    private Integer workspaceId;

    @NotBlank
    private String name;

    @NotNull
    private ConversationEnum type;

    @NotNull
    private Boolean isPrivate = false;

    // Danh sách user IDs để thêm vào conversation (bắt buộc cho DM, optional cho CHANNEL)
    private Set<Integer> memberIds;
}
