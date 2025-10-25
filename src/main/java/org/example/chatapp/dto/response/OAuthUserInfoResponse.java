package org.example.chatapp.dto.response;

import lombok.Data;

@Data
public class OAuthUserInfoResponse {
    private String id;
    private String email;
    private String name;
    private String avatarUrl;
}
