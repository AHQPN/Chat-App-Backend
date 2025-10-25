package org.example.chatapp.service.impl;

import org.example.chatapp.entity.User;
import org.example.chatapp.entity.VerificationCode;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.repository.UserRepository;
import org.example.chatapp.repository.VerificationCodeRepository;
import org.example.chatapp.service.enums.VerificationCodeType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class VerificationCodeService {
    private final VerificationCodeRepository verificationCodeRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    // ====================== TẠO CODE MỚI ======================
    public Optional<VerificationCode> generateNewVerificationCode(User user,VerificationCodeType type) {

        Optional<VerificationCode> existingCode = verificationCodeRepository
                .findByUserAndType(user, type)
                .filter(code -> code.getExpiresAt().isAfter(LocalDateTime.now()));

        if (existingCode.isPresent()) {
            // Đã có mã còn hạn, không tạo mới
            return existingCode;
        }

        // 2️⃣ Xóa mã cũ cùng loại (nếu hết hạn hoặc tồn tại)
        verificationCodeRepository.findByUserAndType(user, type)
                .ifPresent(verificationCodeRepository::delete);

        // 3️⃣ Tạo mã mới
        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setUser(user);
        verificationCode.setType(type);
        verificationCode.setVerificationCode(UUID.randomUUID().toString());
        verificationCode.setCreatedAt(LocalDateTime.now());
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(2));

        verificationCodeRepository.save(verificationCode);
        return Optional.of(verificationCode);
    }

    // ====================== GỬI EMAIL ======================
    public void sendVerificationEmail(User user, String siteURL, VerificationCode verificationCode) {
        String toAddress = user.getEmail();
        String senderName = "SparkMinds";
        String fromAddress = "nguyenbro9721@gmail.com";
        VerificationCodeType type = verificationCode.getType();
        String verifyURL = siteURL + "/verification/verify?code="
                + verificationCode.getVerificationCode()
                + "&type=" + type.name();

        String subject;
        String content;

        switch (type) {
            case EMAIL_VERIFICATION -> {
                subject = "Please verify your registration";
                content = String.format("""
                        Dear %s,<br>
                        Please click the link below to verify your email:<br>
                        <h3><a href="%s" target="_self">VERIFY EMAIL</a></h3>
                        Thank you,<br>%s.
                        """, user.getFullName(), verifyURL, senderName);
            }
            case PASSWORD_RESET -> {
                subject = "Password Reset Request";
                content = String.format("""
                        Dear %s,<br>
                        You requested to reset your password.<br>
                        Click below to continue:<br>
                        <h3><a href="%s" target="_self">RESET PASSWORD</a></h3>
                        Thank you,<br>%s.
                        """, user.getFullName(), verifyURL, senderName);
            }
            case CHANGE_MAIL -> {
                subject = "Confirm your new email";
                content = String.format("""
                        Dear %s,<br>
                        Please verify your new email by clicking the link below:<br>
                        <h3><a href="%s" target="_self">CONFIRM EMAIL</a></h3>
                        Thank you,<br>%s.
                        """, user.getFullName(), verifyURL, senderName);
            }
            default -> throw new AppException(ErrorCode.INVALID_CODE);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);

            helper.setFrom(fromAddress, senderName);
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);

        } catch (UnsupportedEncodingException | MessagingException e) {
            throw new AppException(ErrorCode.FAIL_TO_VERIFY_EMAIL);
        }
    }

    public Boolean verify(String code, VerificationCodeType type) {
        VerificationCode verificationCode = verificationCodeRepository.findByVerificationCode(code)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CODE));

        if (verificationCode.getExpiresAt() != null &&
                verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            verificationCodeRepository.delete(verificationCode);
            throw new AppException(ErrorCode.EXPIRED_VERIFICATION_CODE);
        }

        if (!verificationCode.getType().equals(type)) {
            throw new AppException(ErrorCode.INVALID_CODE);
        }

        User user = verificationCode.getUser();
        switch (type) {
            case EMAIL_VERIFICATION -> {
                if (!user.getIsVerified()) {
                    user.setIsVerified(true);
                    userRepository.save(user);
                }
                break;
            }
            case PASSWORD_RESET, CHANGE_MAIL -> {
                break;
            }
            default -> throw new AppException(ErrorCode.INVALID_CODE);
        }
        verificationCodeRepository.delete(verificationCode);
        return true;

    }
}
