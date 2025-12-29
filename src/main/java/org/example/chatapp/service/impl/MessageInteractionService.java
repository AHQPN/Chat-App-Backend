package org.example.chatapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.AttachFileRequest;
import org.example.chatapp.dto.request.ReactMessageRequest;
import org.example.chatapp.dto.response.MessageInteractionResponse;
import org.example.chatapp.dto.response.MessageResponse;
import org.example.chatapp.entity.*;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.mapper.MessageMapper;
import org.example.chatapp.repository.*;
import org.springframework.scheduling.annotation.Async;
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

    @Async
    public void sendTypingStatus(Integer conversationId, boolean isTyping, Integer userId) {
        try {
            // Dùng JOIN FETCH để eager load User trong 1 query
            ConversationMember member = memberRepository
                    .findByConversationIdAndUserIdWithUser(conversationId, userId)
                    .orElse(null);
            
            if (member == null) {
                return;
            }

            var typingNotification = new java.util.HashMap<String, Object>();
            typingNotification.put("type", "TYPING");
            typingNotification.put("conversationId", conversationId);
            typingNotification.put("userId", userId);
            typingNotification.put("userName", member.getUser().getFullName());
            typingNotification.put("avatar", member.getUser().getAvatar());
            typingNotification.put("isTyping", isTyping);

            webSocketService.sendMessageToConversation(conversationId, typingNotification);
        } catch (Exception e) {
            // Silent fail - typing is not critical
        }
    }


    @Transactional
    public MessageInteractionResponse addReaction(ReactMessageRequest request, Integer messageId, Integer userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        ConversationMember member = memberRepository
                .findByConversation_IdAndUser_UserId(message.getConversation().getId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CONVERSATION));

        Optional<MessageReaction> existingReaction = messageReactionRepository
                .findByMessage_IdAndConversationMember_Id(message.getId(), member.getId());

        var reactionNotification = new java.util.HashMap<String, Object>();
        reactionNotification.put("messageId", message.getId());
        reactionNotification.put("userId", member.getUser().getUserId());
        reactionNotification.put("userName", member.getUser().getFullName());

        if (existingReaction.isPresent()) {
            MessageReaction existing = existingReaction.get();
            
            if (existing.getEmoji().equals(request.getEmoji())) {
                messageReactionRepository.delete(existing);
                
                reactionNotification.put("type", "REACTION_REMOVED");
                webSocketService.sendMessageToConversation(message.getConversation().getId(), reactionNotification);
            } else {

                existing.setEmoji(request.getEmoji());
                existing.setReactedAt(System.currentTimeMillis());
                messageReactionRepository.save(existing);
                
                reactionNotification.put("type", "REACTION_UPDATED");
                reactionNotification.put("emoji", fileService.buildEmojiUrl(existing.getEmoji())); // Build full URL
                reactionNotification.put("reactedAt", existing.getReactedAt());
                webSocketService.sendMessageToConversation(message.getConversation().getId(), reactionNotification);
            }
        } else {
            MessageReaction reaction = MessageReaction.builder()
                    .message(message)
                    .conversationMember(member)
                    .emoji(request.getEmoji())
                    .reactedAt(System.currentTimeMillis())
                    .build();
            messageReactionRepository.save(reaction);
            
            reactionNotification.put("type", "REACTION_ADDED");
            reactionNotification.put("emoji", fileService.buildEmojiUrl(reaction.getEmoji())); // Build full URL
            reactionNotification.put("reactedAt", reaction.getReactedAt());
            webSocketService.sendMessageToConversation(message.getConversation().getId(), reactionNotification);
        }
        
        return buildMessageInteractionResponse(message);
    }


    @Transactional
    public void removeReaction(Integer messageId, Integer userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        
        ConversationMember member = memberRepository
                .findByConversation_IdAndUser_UserId(message.getConversation().getId(), userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CONVERSATION));

        Boolean exists = messageReactionRepository.existsByMessage_IdAndConversationMember_Id(messageId, member.getId());
        if (!exists) {
            throw new AppException(ErrorCode.REACTION_NOT_FOUND);
        }
        
        messageReactionRepository.deleteByMessage_IdAndConversationMember_Id(messageId, member.getId());
        
        // Send WebSocket notification for reaction removal
        var reactionNotification = new java.util.HashMap<String, Object>();
        reactionNotification.put("type", "REACTION_REMOVED");
        reactionNotification.put("messageId", messageId);
        reactionNotification.put("userId", member.getUser().getUserId());
        webSocketService.sendMessageToConversation(message.getConversation().getId(), reactionNotification);
    }


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
        MessageResponse res = MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getUserId())
                .senderName(message.getSender().getFullName())
                .senderAvatar(message.getSender().getAvatar())
                .isPinned(true)
                .build();
        webSocketService.sendMessageToConversation(convId, res);
        return buildMessageInteractionResponse(message);
    }


    @Transactional
    public void unpinMessage(Integer messageId) {
        if (!pinnedMessageRepository.existsByMessage_Id(messageId)) {
            throw new AppException(ErrorCode.NOT_PINNED);
        }
        
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        pinnedMessageRepository.deleteByMessage_Id(messageId);

        MessageResponse response = MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .status(message.getStatus())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSender().getUserId())
                .senderName(message.getSender().getFullName())
                .senderAvatar(message.getSender().getAvatar())
                .isPinned(false)
                .build();
        webSocketService.sendMessageToConversation(message.getConversation().getId(), response);
    }

    public Boolean checkPinLimit(Integer conversationId) {
        long count = pinnedMessageRepository.countByConversation_Id(conversationId);
        return count >= 3;
    }


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

    private MessageInteractionResponse buildMessageInteractionResponse(Message message) {
        List<MessageInteractionResponse.ReactionInfo> reactions = java.util.Collections.emptyList();
        if (message.getReactions() != null && !message.getReactions().isEmpty()) {
            reactions = message.getReactions().stream()
                    .map(r -> new MessageInteractionResponse.ReactionInfo(
                            r.getConversationMember().getUser().getUserId(),
                            fileService.buildEmojiUrl(r.getEmoji()), // Build full URL
                            r.getReactedAt()
                    )).toList();
        }

        List<Integer> mentions = java.util.Collections.emptyList();
        if (message.getMentions() != null && !message.getMentions().isEmpty()) {
            mentions = message.getMentions().stream()
                    .map(m -> m.getConversationMember().getUser().getUserId())
                    .toList();
        }

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
