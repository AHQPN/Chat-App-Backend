package org.example.chatapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageUpdateRequest {
    @NotBlank
    String message;
}
