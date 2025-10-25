package org.example.chatapp.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SignupRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Size(min = 6, max = 20, message = "INVALID_PASSWORD")
    @NotBlank(message = "FIELD_REQUIRED")
    private String password;


    private String phoneNumber;

    @Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$",
            message = "INVALID_EMAIL"
    )
    private String email;

}
