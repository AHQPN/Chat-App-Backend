package org.example.chatapp.controller;

import lombok.AllArgsConstructor;
import org.example.chatapp.dto.response.ApiResponse;
import org.example.chatapp.service.impl.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/files")
@AllArgsConstructor
public class FileController {

    private FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> uploadFiles(@RequestParam("files") List<MultipartFile> files) {
        try {
            if (files.isEmpty()) {
                return ResponseEntity.ok().body(ApiResponse.builder().message("No files uploaded").build());
            }

            List<String> urls = fileService.uploadFiles(files);


            return ResponseEntity.ok().body(ApiResponse.builder().data(urls).build());
        } catch (Exception e) {
            return ResponseEntity.ok().body(ApiResponse.builder().message("Loi: " + e.getMessage()).build());
        }
    }
    @GetMapping
    public ResponseEntity<?> getFiles() {
        return ResponseEntity.ok("Lay ok");
    }

    /**
     * Lấy danh sách emoji URLs (full URLs để hiển thị trực tiếp)
     */
    @GetMapping("/emoji-urls")
    public ResponseEntity<?> getEmojiUrls() {
        return ResponseEntity.ok().body(ApiResponse.builder().data(fileService.getAllEmojiUrls()).build());
    }

    /**
     * Lấy danh sách emoji filenames (chỉ filename để lưu vào DB)
     * Response: ["1F565.png", "1F566.png", ...]
     */
    @GetMapping("/emojis")
    public ResponseEntity<?> getEmojiFilenames() {
        return ResponseEntity.ok().body(ApiResponse.builder().data(fileService.getAllEmojiFilenames()).build());
    }
}
