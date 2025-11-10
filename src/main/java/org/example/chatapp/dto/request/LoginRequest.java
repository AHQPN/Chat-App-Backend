package org.example.chatapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Pattern(
            regexp = "^(\\+84|0)[0-9]{9}$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
            message = "INVALID_EMAIL_OR_PHONE_NUMBER"
    )
    private String identifier;

    @Size(min = 6, max = 20, message = "INVALID_PASSWORD")
    @NotBlank(message = "FIELD_REQUIRED")
    private String password;



}
