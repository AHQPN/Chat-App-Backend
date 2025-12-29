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

    @Value("${emoji.folder}")
    private String emojiFolder;

    @Value("${emoji.base-url}")
    private String emojiBaseUrl;

    private final Storage storage = StorageOptions.getDefaultInstance().getService();

    @Transactional
    public List<String> uploadFiles(List<MultipartFile> files) throws IOException {
        return processUpload(files).stream()
                .map(Attachment::getFileUrl)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Integer> uploadAttachments(List<MultipartFile> files) throws IOException {
        return processUpload(files).stream()
                .map(Attachment::getId)
                .collect(Collectors.toList());
    }

    private List<Attachment> processUpload(List<MultipartFile> files) throws IOException {
        List<Attachment> attachments = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

            BlobId blobId = BlobId.of(bucketName, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(file.getContentType()).build();

            storage.create(blobInfo, file.getBytes());
            String fileUrl = String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);

            Attachment attachment = Attachment.builder()
                    .fileUrl(fileUrl)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedAt(System.currentTimeMillis())
                    .build();

            attachmentRepository.save(attachment);
            attachments.add(attachment);
        }
        return attachments;
    }

    /**
     * Lấy danh sách filenames của emoji (không có URL)
     * Dùng để lưu vào DB chỉ lưu filename
     */
    public List<String> getAllEmojiFilenames() {
        Iterable<Blob> blobs = storage.get(bucketName)
                .list(Storage.BlobListOption.prefix(emojiFolder))
                .iterateAll();

        return StreamSupport.stream(blobs.spliterator(), false)
                .filter(blob -> !blob.getName().equals(emojiFolder))
                .map(blob -> blob.getName().replace(emojiFolder, "")) // Chỉ lấy filename
                .collect(Collectors.toList());
    }

    /**
     * Build full URL từ emoji filename
     * Dùng khi query từ DB và cần trả về URL cho frontend
     */
    public String buildEmojiUrl(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        // Nếu đã là URL đầy đủ, trả về nguyên
        if (filename.startsWith("http://") || filename.startsWith("https://")) {
            return filename;
        }
        return emojiBaseUrl + filename;
    }

    /**
     * Lấy danh sách full URLs của emoji
     * Dùng cho API GET /files/emoji-urls
     */
    @Transactional
    public List<String> getAllEmojiUrls() {
        return getAllEmojiFilenames().stream()
                .map(this::buildEmojiUrl)
                .collect(Collectors.toList());
    }
}
