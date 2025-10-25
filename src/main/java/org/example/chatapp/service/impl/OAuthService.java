package org.example.chatapp.service.impl;

import org.example.chatapp.dto.response.OAuthUserInfoResponse;
import org.example.chatapp.service.enums.AuthProviderEnum;
import org.example.chatapp.service.inter.OAuthProviderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class OAuthService {
    private final Map<AuthProviderEnum, OAuthProviderService> providerServiceMap;

    public OAuthService(List<OAuthProviderService> services) {
        this.providerServiceMap = services.stream()
                .collect(Collectors.toMap(
                        OAuthProviderService::getProvider,
                        s -> s
                ));
    }

    public String exchangeCodeForAccessToken(String code, AuthProviderEnum provider) {
        return providerServiceMap.get(provider).exchangeCodeForAccessToken(code);
    }

    public OAuthUserInfoResponse getUserInfo(String accessToken, AuthProviderEnum provider) {
        return providerServiceMap.get(provider).getUserInfo(accessToken);
    }
}
