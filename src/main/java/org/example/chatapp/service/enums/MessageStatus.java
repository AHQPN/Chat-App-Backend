package org.example.chatapp.service.enums;

public enum MessageStatus {
    SENT,       // Gửi bình thường, không xoá
    REVOKED,    // Đã thu hồi (hiển thị "Tin nhắn đã thu hồi" cho mọi người)
    DELETED     // Đã xoá (có thể ẩn hẳn hoặc hiển thị đã xoá phía người gửi)
}
