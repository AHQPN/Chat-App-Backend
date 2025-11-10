package org.example.chatapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.FileUploadRequest;
import org.example.chatapp.entity.*;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MessageInteractionService {
    private final MessageRepository messageRepository;
    private final ConversationMemberRepository memberRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final MessageMentionRepository mentionRepository;
    private final PinnedMessageRepository pinnedMessageRepository;
    private final AttachmentRepository attachmentRepository;
    private final FileService fileService;

    @Transactional
    public void addReaction(Integer messageId, Integer memberId, String emoji) {

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        ConversationMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CONVERSATION));

        if (!member.getConversation().getId().equals(message.getConversation().getId())) {
            throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
        }

        Optional<MessageReaction> existed = messageReactionRepository
                .findByMessage_IdAndConversationMember_Id(messageId, memberId);

        MessageReaction reaction = existed
                .map(r -> {
                    r.setEmoji(emoji);
                    r.setReactedAt(System.currentTimeMillis());
                    return r;
                })
                .orElseGet(() -> MessageReaction.builder()
                        .message(message)
                        .conversationMember(member)
                        .emoji(emoji)
                        .reactedAt(System.currentTimeMillis())
                        .build());

        messageReactionRepository.save(reaction);

    }

    // ===========================
    // REMOVE REACTION
    // ===========================
    @Transactional
    public void removeReaction(Integer messageId, Integer memberId) {

        boolean exists = messageReactionRepository.existsByMessage_IdAndConversationMember_Id(messageId, memberId);
        if (!exists) {
            throw new AppException(ErrorCode.REACTION_NOT_FOUND);
        }

        messageReactionRepository.deleteByMessage_IdAndConversationMember_Id(messageId, memberId);
    }

    // ===========================
    // ✅ 3) MENTION USERS
    // ===========================
    @Transactional
    public void mention(Integer messageId, List<Integer> memberIds) {

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.WORKSPACE_NOT_FOUND));

        // kiểm tra từng member
        List<MessageMention> mentions = memberIds.stream().map(id -> {

            ConversationMember member = memberRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CONVERSATION));

            // kiểm tra thuộc cùng conversation
            if (!member.getConversation().getId().equals(message.getConversation().getId())) {
                throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
            }

            return MessageMention.builder()
                    .message(message)
                    .conversationMember(member)
                    .build();
        }).toList();

        mentionRepository.saveAll(mentions);
    }

    // ===========================
    //  4) PIN MESSAGE
    // ===========================
    @Transactional
    public void pinMessage(Integer messageId, Integer conversationId, Integer pinnedByMemberId) {

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // tin nhắn không thuộc conversation này
        if (!message.getConversation().getId().equals(conversationId)) {
            throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
        }

        ConversationMember pinnedBy = memberRepository.findById(pinnedByMemberId)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND));

        // check thành viên này có trong conversation
        if (!pinnedBy.getConversation().getId().equals(conversationId)) {
            throw new AppException(ErrorCode.MESSAGE_ALREADY_PINNED);
        }

        if (pinnedMessageRepository.existsByMessage_Id(messageId)) {
            throw new AppException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        PinnedMessage pin = PinnedMessage.builder()
                .message(message)
                .conversation(message.getConversation())
                .pinnedBy(pinnedBy)
                .pinnedAt(System.currentTimeMillis())
                .build();

        pinnedMessageRepository.save(pin);
    }

    // ===========================
    // ✅ 5) UNPIN
    // ===========================
    @Transactional
    public void unpinMessage(Integer messageId) {
        if (!pinnedMessageRepository.existsByMessage_Id(messageId)) {
            throw new AppException(ErrorCode.NOT_PINNED);
        }
        pinnedMessageRepository.deleteByMessage_Id(messageId);
    }

    // ===========================
    // ✅ 6) UPLOAD ATTACHMENT
    // ===========================
    @Transactional
    public Attachment uploadAttachment(Integer messageId, FileUploadRequest request) throws IOException {

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            return null;
        }

        // TODO: upload file lên cloud / local storage
        String url = fileService.uploadFile(file);

        Attachment attachment = Attachment.builder()
                .message(message)
                .fileUrl(url)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedAt(System.currentTimeMillis())
                .build();

        return attachmentRepository.save(attachment);
    }



}
