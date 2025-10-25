package org.example.chatapp.controller;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.response.ApiResponse;
import org.example.chatapp.service.impl.FileService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@AllArgsConstructor
public class FileController {

    private FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String url = fileService.uploadFile(file);
            return ResponseEntity.ok().body(ApiResponse.builder().data(url).build());
        } catch (Exception e) {
            return ResponseEntity.ok().body(ApiResponse.builder().message("Loi").build());
        }
    }
}
