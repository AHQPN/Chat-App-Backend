package org.example.chatapp.controller;

import lombok.AllArgsConstructor;
import org.example.chatapp.dto.request.AddMemberToWorkspaceRequest;
import org.example.chatapp.dto.request.WorkspaceRequest;
import org.example.chatapp.dto.response.ApiResponse;
import org.example.chatapp.entity.Workspace;
import org.example.chatapp.security.model.UserDetailsImpl;
import org.example.chatapp.service.impl.WorkspaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workspaces")
@AllArgsConstructor
public class WorkspaceController {
    private final WorkspaceService workspaceService;
    private final AuthenticationManager authenticationManager;

    @PostMapping()
    public ResponseEntity<?> createWorkspace(@RequestBody WorkspaceRequest workspace,@AuthenticationPrincipal UserDetailsImpl principal) {

        Workspace workspace1 = workspaceService.createNewWorkspace(workspace, principal.getId());
        return ResponseEntity.ok().body(ApiResponse.builder().data(workspace1).build());
    }

    @PostMapping("/add-member")
    public ResponseEntity<?> addMemberToWorkspace(@RequestBody AddMemberToWorkspaceRequest request,@AuthenticationPrincipal UserDetailsImpl principal) {
        workspaceService.addMemberToWorkspace(request,principal.getId());
        return ResponseEntity.ok().body(ApiResponse.builder().message("Thêm thành viên thành công").build());
    }

    @GetMapping("/my-workspaces")
    public ResponseEntity<ApiResponse<List<Workspace>>> getMyWorkspaces() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<Workspace> workspaces = workspaceService.getWorkspacesByUser(userDetails.getId());

        return ResponseEntity.ok().body(ApiResponse.<List<Workspace>>builder().data(workspaces).build());
    }


}
