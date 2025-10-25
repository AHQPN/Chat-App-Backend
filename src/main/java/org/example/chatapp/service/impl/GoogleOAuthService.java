package org.example.chatapp.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.example.chatapp.dto.response.OAuthUserInfoResponse;
import org.example.chatapp.service.enums.AuthProviderEnum;
import org.example.chatapp.service.inter.OAuthProviderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleOAuthService implements OAuthProviderService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    @Override
    public String exchangeCodeForAccessToken(String code) {
        try {
            return new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    clientId,
                    clientSecret,
                    code,
                    redirectUri
            ).execute().getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Google access token", e);
        }
    }

    @Override
    public OAuthUserInfoResponse getUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" + accessToken;

        Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
        OAuthUserInfoResponse info = new OAuthUserInfoResponse();
        info.setId((String) resp.get("id"));
        info.setEmail((String) resp.get("email"));
        info.setName((String) resp.get("name"));
        return info;
    }

    @Override
    public AuthProviderEnum getProvider() {
        return AuthProviderEnum.GOOGLE;
    }

}
