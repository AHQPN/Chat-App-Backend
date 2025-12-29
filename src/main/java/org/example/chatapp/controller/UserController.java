package org.example.chatapp.controller;

import org.example.chatapp.dto.request.SignupRequest;
import org.example.chatapp.dto.request.UpdateProfileRequest;
import org.example.chatapp.dto.response.ApiResponse;
import org.example.chatapp.dto.response.UserResponse;
import org.example.chatapp.entity.User;
import org.example.chatapp.security.model.UserDetailsImpl;
import org.example.chatapp.service.impl.UserService;
import org.example.chatapp.service.enums.RoleEnum;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    @Value("${APP_SITE_URL}")
    private String siteUrl;

    // Constructor injection
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // POST /users → tạo user mới
    @PostMapping
    public ResponseEntity<ApiResponse> addUser(@RequestBody @Valid SignupRequest signupRequest, HttpServletRequest httpServletRequest) {

        User user = userService.createUser(signupRequest, RoleEnum.Admin, false, siteUrl); // tạo user

        return ResponseEntity.ok().body(ApiResponse.builder().message("User created successfully").build());
    }

    @PutMapping("{id}")
    public ResponseEntity<ApiResponse> updateUser() {
        return ResponseEntity.ok().body(ApiResponse.builder().message("User updated successfully").build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getUsers(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PageableDefault(sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<User> usersPage = userService.getAllUsers(principal != null ? principal.getId() : null, pageable);

        Page<UserResponse> responsePage = usersPage.map(user -> UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .build());

        return ResponseEntity.ok().body(ApiResponse.builder()
                .message("Get users successfully")
                .data(responsePage)
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String name) {
        List<UserResponse> users = userService.searchUsersByName(name).stream()
                .map(user -> UserResponse.builder()
                        .userId(user.getUserId())
                        .fullName(user.getFullName())
                        .avatar(user.getAvatar())
                        .build())
                .toList();
        return ResponseEntity.ok(users);
    }

    // GET /users/me → lấy profile của user hiện tại
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(@AuthenticationPrincipal UserDetailsImpl principal) {
        User user = userService.getUserById(principal.getId());
        UserResponse response = UserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .avatar(user.getAvatar())
                .build();
        return ResponseEntity.ok(response);
    }

    // PUT /users/me → cập nhật profile của user hiện tại
    @PutMapping("/me")
    public ResponseEntity<ApiResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestBody UpdateProfileRequest request) {
        
        userService.updateProfile(principal.getId(), request);
        
        return ResponseEntity.ok().body(ApiResponse.builder()
                .message("Profile updated successfully")
                .build());
    }

}
