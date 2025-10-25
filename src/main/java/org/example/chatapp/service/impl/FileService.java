package org.example.chatapp.service.impl;

import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class FileService {

    @Value("${gcp.bucket.name}")
    private String bucketName;

    @Value("${file.upload.max-size}")
    private long maxFileSize;

    private final Storage storage = StorageOptions.getDefaultInstance().getService();

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();


        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();

        storage.create(blobInfo, file.getBytes());

        // Trả về URL public (nếu bucket public) hoặc dùng Signed URL nếu private
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
    }
}
