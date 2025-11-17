package org.example.chatapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.AttachFileRequest;
import org.example.chatapp.dto.request.ReactMessageRequest;
import org.example.chatapp.dto.response.MessageInteractionResponse;
import org.example.chatapp.dto.response.PinMessageResponse;
import org.example.chatapp.entity.*;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.mapper.MessageMapper;
import org.example.chatapp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserRepository userRepository;
    private final FileService fileService;
    private final WebSocketService webSocketService;
    private final MessageMapper messageMapper;
    // ===========================
    // 1) ADD / UPDATE REACTION
    // ===========================
    @Transactional
    public MessageInteractionResponse addReaction(ReactMessageRequest request,Integer messageId, Integer userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        ConversationMember member = memberRepository
                .findByConversation_IdAndUser_UserId(message.getConversation().getId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CONVERSATION));

        Optional<MessageReaction> existingReaction = messageReactionRepository
                .findByMessage_IdAndConversationMember_Id(message.getId(), member.getId());

        MessageReaction reaction = existingReaction
                .map(r -> {
                    r.setEmoji(request.getEmoji());
                    r.setReactedAt(System.currentTimeMillis());
                    return r;
                })
                .orElseGet(() -> MessageReaction.builder()
                        .message(message)
                        .conversationMember(member)
                        .emoji(request.getEmoji())
                        .reactedAt(System.currentTimeMillis())
                        .build());

        messageReactionRepository.save(reaction);
        webSocketService.sendMessageToConversation(message.getConversation().getId(), reaction.getEmoji());
        return buildMessageInteractionResponse(message);
    }

    // ===========================
    // 2) REMOVE REACTION
    // ===========================
    @Transactional
    public void removeReaction(Integer messageId, Integer userId) {
        ConversationMember member = memberRepository
                .findByConversation_IdAndUser_UserId(
                        messageRepository.findById(messageId)
                                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND))
                                .getConversation().getId(),
                        userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CONVERSATION));

        Boolean exists = messageReactionRepository.existsByMessage_IdAndConversationMember_Id(messageId, member.getId());
        if (!exists) {
            throw new AppException(ErrorCode.REACTION_NOT_FOUND);
        }
        messageReactionRepository.deleteByMessage_IdAndConversationMember_Id(messageId, member.getId());
        webSocketService.sendMessageToConversation(messageRepository.findById(messageId).get().getConversation().getId(), null);
    }

    // ===========================
    // 3) MENTION USERS
    // ===========================
    @Transactional
    public void mention(Integer messageId, List<Integer> memberIds) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        List<MessageMention> mentions = memberIds.stream().map(id -> {
            ConversationMember member = memberRepository.findById(id)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CONVERSATION));

            if (!member.getConversation().getId().equals(message.getConversation().getId())) {
                throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
            }

            return MessageMention.builder()
                    .message(message)
                    .conversationMember(member)
                    .build();
        }).toList();

        mentionRepository.saveAll(mentions);
        webSocketService.sendMessageToConversation(message.getConversation().getId(), mentions);


    }

    // ===========================
    // 4) PIN MESSAGE
    // ===========================
    @Transactional
    public MessageInteractionResponse pinMessage(Integer messageId, Integer userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        ConversationMember member = memberRepository
                .findByConversation_IdAndUser_UserId(message.getConversation().getId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CONVERSATION));

        if (pinnedMessageRepository.existsByMessage_Id(messageId)) {
            throw new AppException(ErrorCode.MESSAGE_ALREADY_PINNED);
        }

        PinnedMessage pinnedMessage = PinnedMessage.builder()
                .message(message)
                .conversation(message.getConversation())
                .pinnedBy(member)
                .pinnedAt(System.currentTimeMillis())
                .build();

        pinnedMessageRepository.save(pinnedMessage);
        Integer convId = message.getConversation().getId();
        PinMessageResponse pinMessageResponse = messageMapper.toPinMessage(pinnedMessage);
        webSocketService.sendMessageToConversation(convId, pinMessageResponse);
        return buildMessageInteractionResponse(message);
    }

    // ===========================
    // 5) UNPIN MESSAGE
    // ===========================
    @Transactional
    public void unpinMessage(Integer messageId) {
        if (!pinnedMessageRepository.existsByMessage_Id(messageId)) {
            throw new AppException(ErrorCode.NOT_PINNED);
        }
        pinnedMessageRepository.deleteByMessage_Id(messageId);
        webSocketService.sendMessageToConversation(messageRepository.findById(messageId).get().getConversation().getId()
                ,"Unpin successfully");
    }

    // ===========================
    // 6) UPLOAD ATTACHMENT
    // ===========================
    @Transactional
    public Attachment uploadAttachment(AttachFileRequest request) throws IOException {
        Message message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));


        List<String> fileList = request.getFileUrl();
        List<Attachment> savedAttachments = fileList.stream()
                .map(url -> Attachment.builder()
                        .message(message)
                        .fileUrl(url)
                        .fileType("placeholder/type")
                        .fileSize(0L)
                        .uploadedAt(System.currentTimeMillis())
                        .build())
                .map(attachmentRepository::save)
                .toList();

        return savedAttachments.get(savedAttachments.size() - 1);


    }

    // ===========================
    // PRIVATE HELPER: BUILD RESPONSE DTO
    // ===========================
    private MessageInteractionResponse buildMessageInteractionResponse(Message message) {
        List<MessageInteractionResponse.ReactionInfo> reactions = message.getReactions().stream()
                .map(r -> new MessageInteractionResponse.ReactionInfo(
                        r.getConversationMember().getUser().getUserId(),
                        r.getEmoji(),
                        r.getReactedAt()
                )).toList();

        List<Integer> mentions = message.getMentions().stream()
                .map(m -> m.getConversationMember().getUser().getUserId())
                .toList();

        return MessageInteractionResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .reactions(reactions)
                .mentions(mentions)
                .pinned(pinnedMessageRepository.existsByMessage_Id(message.getId()))
                .createdAt(message.getCreatedAt())
                .build();
    }
}
