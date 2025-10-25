package org.example.chatapp.service.inter;

import org.example.chatapp.dto.response.OAuthUserInfoResponse;
import org.example.chatapp.service.enums.AuthProviderEnum;

public interface OAuthProviderService {
    // Dùng code để lấy access token
    String exchangeCodeForAccessToken(String code);
    // Dùng access token để lấy thông tin user
    OAuthUserInfoResponse getUserInfo(String accessToken);
    AuthProviderEnum getProvider();
}
