package org.example.chatapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
    private Integer userId;
    private String fullName;
    private String avatar;
}
