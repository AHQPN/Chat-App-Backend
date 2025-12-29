package org.example.chatapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.SignupRequest;
import org.example.chatapp.dto.request.UpdateProfileRequest;
import org.example.chatapp.dto.response.OAuthUserInfoResponse;
import org.example.chatapp.entity.User;
import org.example.chatapp.entity.VerificationCode;
import org.example.chatapp.entity.WorkspaceMember;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.repository.UserRepository;
import org.example.chatapp.repository.VerificationCodeRepository;
import org.example.chatapp.repository.WorkspaceMemberRepository;
import org.example.chatapp.repository.WorkspaceRepository;
import org.example.chatapp.service.enums.AuthProviderEnum;
import org.example.chatapp.service.enums.RoleEnum;
import org.example.chatapp.service.enums.VerificationCodeEnum;
import org.example.chatapp.service.enums.WorkspaceRoleEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeService verificationCodeService;
    private final VerificationCodeRepository verificationCodeRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
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
        userRepository.save(user);
        if (!user.getIsVerified())
        {
             Optional<VerificationCode>  verificationCode = verificationCodeService.generateNewVerificationCode(user, VerificationCodeEnum.EMAIL_VERIFICATION);
             verificationCodeService.sendVerificationEmail(user,siteURL,verificationCode.get());
        }

        return user;
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
        
        // Save user trước để có ID
        User savedUser = userRepository.save(user);

        WorkspaceMember workspaceMember = new WorkspaceMember();
        workspaceMember.setUser(savedUser);
        workspaceMember.setJoinedAt(System.currentTimeMillis());
        workspaceMember.setRole(WorkspaceRoleEnum.MEMBER);
        workspaceMember.setWorkspace(workspaceRepository.getWorkspaceById(5));
        workspaceMemberRepository.save(workspaceMember);
        
        return savedUser;
    }


    public Optional<User> getUserByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return Optional.empty();
        }
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



    public List<User> searchUsersByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return List.of();
        }
        return userRepository.findByFullNameContainingIgnoreCaseAndUserTypeNot(name.trim(), RoleEnum.Admin);
    }

    @Transactional
    public User updateProfile(Integer userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Update fullName nếu có
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        // Update phoneNumber nếu có
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
            // Kiểm tra phoneNumber đã tồn tại chưa (trừ user hiện tại)
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber()) 
                    && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
                throw new AppException(ErrorCode.USER_PHONE_OR_EMAIL_EXIST);
            }
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        // Update avatar nếu có
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        return userRepository.save(user);
    }

    public User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    public Page<User> getAllUsers(Integer excludeUserId, Pageable pageable) {
        if (excludeUserId == null) {
            return userRepository.findByUserTypeNot(RoleEnum.Admin, pageable);
        }
        return userRepository.findByUserIdNotAndUserTypeNot(excludeUserId, RoleEnum.Admin, pageable);
    }

}

