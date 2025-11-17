package org.example.chatapp.service.impl;

import com.google.cloud.storage.*;
import lombok.RequiredArgsConstructor;
import org.example.chatapp.entity.Attachment;
import org.example.chatapp.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class FileService {
    private final AttachmentRepository attachmentRepository;

    @Value("${gcp.bucket.name}")
    private String bucketName;

    @Value("${file.upload.max-size}")
    private long maxFileSize;

    private static String emojiFolder = "chat emoji/";
    private final Storage storage = StorageOptions.getDefaultInstance().getService();

    @Transactional
    public List<String> uploadFiles(List<MultipartFile> files) throws IOException {
        List<String> fileUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;  // Bỏ qua file rỗng nếu có
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();

            storage.create(blobInfo, file.getBytes());
            String fileUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);

            // Tạo entity Attachment và set thông tin
            Attachment attachment = Attachment.builder()
                    .fileUrl(fileUrl)
                    .fileType(file.getContentType())  // Ví dụ: "image/jpeg"
                    .fileSize(file.getSize())         // Kích thước bytes
                    .uploadedAt(System.currentTimeMillis())  // Timestamp
                    .build();

            // Lưu vào DB
            attachmentRepository.save(attachment);

            fileUrls.add(fileUrl);
        }

        return fileUrls;
    }

    @Transactional
    public List<String> getAllEmojiUrls() {

        Iterable<Blob> blobs = storage.get(bucketName)
                .list(Storage.BlobListOption.prefix(emojiFolder))
                .iterateAll();

        return StreamSupport.stream(blobs.spliterator(), false)
                .filter(blob -> !blob.getName().equals(emojiFolder))
                .map(Blob::getMediaLink)
                .collect(Collectors.toList());
    }

}
