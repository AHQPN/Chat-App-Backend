package org.example.chatapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.SignupRequest;
import org.example.chatapp.dto.response.OAuthUserInfoResponse;
import org.example.chatapp.entity.User;
import org.example.chatapp.entity.VerificationCode;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.repository.UserRepository;
import org.example.chatapp.service.enums.AuthProviderEnum;
import org.example.chatapp.service.enums.RoleEnum;
import org.example.chatapp.service.enums.VerificationCodeType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    VerificationCodeService verificationCodeService;
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;
    @Value("${spring.security.oauth2.client.registration.google.scope}")
    private String googleScope;

    public User createUser(SignupRequest signupRequest, RoleEnum userType, Boolean sendVerification, String siteURL) {

        String phoneNumber = signupRequest.getPhoneNumber();

        if (userRepository.existsByPhoneNumber(phoneNumber) || userRepository.existsByEmail(signupRequest.getEmail())) {
            throw new AppException(ErrorCode.USER_PHONE_OR_EMAIL_EXIST);
        }

        User user = new User();

        user.setIsVerified(!sendVerification);
        user.setFullName(signupRequest.getFullName());
        user.setEmail(signupRequest.getEmail());
        user.setPhoneNumber(phoneNumber);
        user.setUserType(userType);
        user.setCreatedAt(LocalDateTime.now());
        user.setPasswordHash(passwordEncoder.encode(signupRequest.getPassword()));
        if (!user.getIsVerified())
        {
             Optional<VerificationCode>  verificationCode = verificationCodeService.generateNewVerificationCode(user, VerificationCodeType.EMAIL_VERIFICATION);
             verificationCodeService.sendVerificationEmail(user,siteURL,verificationCode.get());
        }

        return userRepository.save(user);
    }

    @Transactional
    public User createUserFromOAuth(OAuthUserInfoResponse userInfo, AuthProviderEnum provider) {
        // Kiểm tra xem user đã tồn tại chưa (theo email)
        Optional<User> usr = userRepository.findByEmail(userInfo.getEmail());
        if (usr.isPresent()) {
            return usr.get();
        }

        User user = new User();
        user.setFullName(userInfo.getName());
        user.setEmail(userInfo.getEmail());

        user.setUserType(RoleEnum.User);
        user.setCreatedAt(LocalDateTime.now());
        user.setIsVerified(true);
        user.setProvider(provider);
        user.setPasswordHash(null);
        user.setProviderId(userInfo.getId());
        return userRepository.save(user);
    }


    public Optional<User> getUserByIdentifier(String identifier) {
        Optional<User> usr = userRepository.findByPhoneNumberOrEmail(identifier,identifier);
        return usr;
    }
    public String generateAuthUrl(AuthProviderEnum authProviderEnum)
    {
        switch (authProviderEnum) {
            case GOOGLE:

                String scopeString = googleScope.replace(",", " ");
                return UriComponentsBuilder.fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                        .queryParam("client_id", googleClientId)
                        .queryParam("redirect_uri", googleRedirectUri)
                        .queryParam("response_type", "code")
                        .queryParam("scope", scopeString)
                        .queryParam("access_type", "offline")
                        .queryParam("prompt", "consent")
                        .toUriString();

//            case FACEBOOK:
//                String fbClientId = "_FACEBOOK_APP_ID";
//                String fbRedirectUri = "http://localhost:8080/auth/oauth2/callback/facebook";
//                String fbScope = "email public_profile";
//                return UriComponentsBuilder.fromHttpUrl("https://www.facebook.com/v16.0/dialog/oauth")
//                        .queryParam("client_id", fbClientId)
//                        .queryParam("redirect_uri", fbRedirectUri)
//                        .queryParam("response_type", "code")
//                        .queryParam("scope", fbScope)
//                        .toUriString();

            default:
                throw new IllegalArgumentException("Unsupported auth provider: " + authProviderEnum);
        }
    }



}
