package org.example.chatapp.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.LoginRequest;
import org.example.chatapp.dto.response.ApiResponse;
import org.example.chatapp.dto.response.LoginResponse;
import org.example.chatapp.dto.response.OAuthUserInfoResponse;
import org.example.chatapp.dto.response.TokenRefreshResponse;
import org.example.chatapp.entity.RefreshToken;
import org.example.chatapp.entity.User;
import org.example.chatapp.entity.VerificationCode;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.security.jwt.JwtUtils;
import org.example.chatapp.security.model.UserDetailsImpl;
import org.example.chatapp.service.enums.VerificationCodeEnum;
import org.example.chatapp.service.impl.OAuthService;
import org.example.chatapp.service.impl.RefreshTokenService;
import org.example.chatapp.service.impl.UserService;
import org.example.chatapp.service.enums.AuthProviderEnum;
import org.example.chatapp.service.enums.RoleEnum;
import org.example.chatapp.service.impl.VerificationCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.example.chatapp.dto.request.SignupRequest;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final OAuthService oAuthService;
    private final VerificationCodeService verificationCodeService;


    // ===== LOGIN =====
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getIdentifier(), loginRequest.getPassword())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        if (!userDetails.isVerified()) {
            throw new AppException(ErrorCode.USER_NOT_VERIFIED);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtUtils.generateJwtToken(authentication);

        // Tạo refresh token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getId());

        // Set HTTP-only cookie cho refresh token
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/auth/refreshtoken")
                .maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        LoginResponse loginResponse = new LoginResponse(accessToken,refreshToken.getRefreshToken(),userDetails.getRole(),userDetails.getId());
        return ResponseEntity.ok().body(ApiResponse.builder().message("Login Successfully").data(loginResponse).build());
    }

    // Đang hardcode state, chưa dùng đúng chức năng của state trong oauth2 để chống CSRF, em sẽ tham khảo xử lý sau
    @GetMapping("/social-login")
    public ResponseEntity<ApiResponse> socialAuth(@RequestParam("login_type") String loginType,HttpServletRequest request)
    {
        AuthProviderEnum authProviderEnum = AuthProviderEnum.valueOf(loginType.trim().toUpperCase());

        String state = URLEncoder.encode("login_type=" + loginType, StandardCharsets.UTF_8);
        String url = userService.generateAuthUrl(authProviderEnum) + "&state=" + state;

        Map<String, String> responseUrl = Collections.singletonMap("authUrl", url);
        return ResponseEntity.ok().body(ApiResponse.builder().data(responseUrl).build());
    }



    @GetMapping("/social-login/callback")
    public ResponseEntity<ApiResponse> socialCallBack(@RequestParam("code") String code , @RequestParam("state") String state,HttpServletRequest request)
    {
        Map<String, String> params = Arrays.stream(state.split("&"))
                .map(s -> s.split("="))
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));


        String loginType = params.get("login_type");
        AuthProviderEnum authProvider = AuthProviderEnum.valueOf(loginType.toUpperCase());

        //Đổi code lấy accesstoken từ gg
        String accessToken = oAuthService.exchangeCodeForAccessToken(code, authProvider);

        //Trích xuất thông tin của user thông qua access token vừa lấy
        OAuthUserInfoResponse userInfo = oAuthService.getUserInfo(accessToken, authProvider);

        // Tạo user mới nếu chưa có , nếu có rồi thì trả ve user hiện tại
        User user = userService.createUserFromOAuth(userInfo, authProvider);
        // Sinh access token của chat app
        String jwt = jwtUtils.generateTokenFromIdentifier(user.getEmail());


        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUserId());

        LoginResponse loginResponse = new LoginResponse(jwt,refreshToken.getRefreshToken(),RoleEnum.User.name(),user.getUserId());
        return ResponseEntity.ok().body(ApiResponse.builder().message("Login Successfully").data(loginResponse).build());
    }
    // ===== SIGNUP =====
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody SignupRequest signUpRequest, HttpServletRequest request, @Value("${APP_SITE_URL}") String siteUrl) {


        User user = userService.createUser(signUpRequest, RoleEnum.User, true, siteUrl);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.builder()
                        .message("We have sent a verification email, please check your inbox")
                        .build());
    }



    // ===== REFRESH TOKEN =====
    @PostMapping("/refreshtoken")
    public ResponseEntity<ApiResponse> refreshToken(@CookieValue(name = "refreshToken", required = false) String requestRefreshToken,
                                                    HttpServletResponse response) {

        if (requestRefreshToken == null) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        RefreshToken refreshToken = refreshTokenService.findByToken(requestRefreshToken)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        refreshTokenService.verifyExpiration(refreshToken);

        User user = refreshToken.getUser();

        refreshTokenService.setRevoked(refreshToken);
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getUserId());

        String accessToken = jwtUtils.generateTokenFromIdentifier(user.getPhoneNumber());

        // Set new refresh token cookie
        ResponseCookie cookie = ResponseCookie.from("refreshToken", newRefreshToken.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/auth/refreshtoken")
                .maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        TokenRefreshResponse tokenRefreshResponse = new TokenRefreshResponse(accessToken,newRefreshToken.getRefreshToken());

        return ResponseEntity.ok().body(ApiResponse.builder().message("New Refresh Token and Access Token are created Successfully").data(tokenRefreshResponse).build());
    }

    // ===== LOGOUT =====
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletResponse response) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String identifier = authentication.getName();
            User user = userService.getUserByIdentifier(identifier)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            refreshTokenService.deleteByUserId(user.getUserId());
            SecurityContextHolder.clearContext();


            ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                    .httpOnly(true)
                    .secure(false)
                    .path("/auth/refreshtoken")
                    .maxAge(0)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return ResponseEntity.ok(ApiResponse.builder().message("Logout successful").build());
    }

    // ===== Resend verified code  =====
    @PostMapping("/resend")
    public ResponseEntity<String> sendVerification(
            @RequestParam String email,
            @RequestParam VerificationCodeEnum type,
            @Value("${APP_SITE_URL}") String siteUrl) {

        // 1️⃣ Tìm user theo email
        Optional<User> optionalUser = userService.getUserByIdentifier(email);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }
        User user = optionalUser.get();

        // 2️⃣ Tạo mã xác minh mới
        Optional<VerificationCode> optionalCode = verificationCodeService.generateNewVerificationCode(user, type);


        // 3️⃣ Gửi email xác minh
        optionalCode.ifPresent(code ->
                verificationCodeService.sendVerificationEmail(user, siteUrl, code)
        );

        return ResponseEntity.ok("Verification email sent successfully!");
    }

    // ====================== XÁC MINH CODE ======================
    @GetMapping("/verify")
    public ResponseEntity<String> verifyCode(
            @RequestParam String code,
            @RequestParam VerificationCodeEnum type) {

        ApiResponse result = verificationCodeService.verify(code, type);
        boolean success = result.getCode() == 1000;
        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Verification Status</title>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; background-color: #f5f6fa; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }\n" +
                "        .container { background-color: #fff; padding: 40px; border-radius: 8px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); text-align: center; }\n" +
                "        h1 { color: " + (success ? "#2ecc71" : "#e74c3c") + "; margin-bottom: 20px; }\n" +
                "        p { font-size: 16px; color: #333; margin-bottom: 30px; }\n" +
                "        a.button { display: inline-block; text-decoration: none; background-color: #3498db; color: #fff; padding: 12px 24px; border-radius: 6px; transition: background-color 0.3s ease; }\n" +
                "        a.button:hover { background-color: #2980b9; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <h1>" + (success ? "Verification Successful!" : "Verification Failed") + "</h1>\n" +
                "        <p>" + result.getMessage() + "</p>\n" +
                "        <a class=\"button\" href=\"http://localhost:3000\">Go Back to App</a>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";

        return ResponseEntity.ok().body(html);
    }
}
