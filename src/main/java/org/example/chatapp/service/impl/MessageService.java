package org.example.chatapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.CreateMessageRequest;
import org.example.chatapp.dto.request.MessageUpdateRequest;
import org.example.chatapp.dto.response.MessageResponse;
import org.example.chatapp.dto.response.MessageSearchResponse;
import org.example.chatapp.entity.*;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.mapper.MessageMapper;
import org.example.chatapp.repository.*;
import org.example.chatapp.service.enums.ConversationRoleEnum;
import org.example.chatapp.service.enums.MessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageMapper messageMapper;
    private final AttachmentRepository attachmentRepository;
    private final MessageMentionRepository messageMentionRepository;
    private final WebSocketService webSocketService;
    private final FileService fileService;
    private final PinnedMessageRepository pinnedMessageRepository;
    private final HiddenMessageRepository hiddenMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createMessage(CreateMessageRequest request,Integer memberId, Integer conversationId,Integer parentMessageId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        if(!conversationMemberRepository.existsByIdAndConversation_Id(memberId, conversationId))
            throw new AppException(ErrorCode.MEMBER_NOT_FOUND);

        ConversationMember sender = conversationMemberRepository.getConversationMembersById(memberId);
        if(sender.getRole() == ConversationRoleEnum.DELETED)
            throw new AppException(ErrorCode.FORBIDDEN);
        Message message = messageMapper.convertToMessage(request);
        message.setCreatedAt(System.currentTimeMillis());
        message.setConversation(conversation);
        message.setSender(sender.getUser());
        message.setStatus(MessageStatus.SENT);

        if(parentMessageId != null){
            Message parentMessage = messageRepository.findById(parentMessageId)
                    .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
            message.setParentMessage(parentMessage);
        }

        if (request.getThreadId() != null) {
            Message rootThread = messageRepository.findById(request.getThreadId())
                    .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
            
            if (!rootThread.getConversation().getId().equals(conversationId)) {
                throw new AppException(ErrorCode.MESSAGE_NOT_IN_CONVERSATION);
            }
            
            message.setThread(rootThread);
        }
        
        messageRepository.save(message);
        
        if (request.getThreadId() != null) {
            messageRepository.incrementThreadReplyCount(request.getThreadId());
        }
        
        List<Attachment> attachments = new ArrayList<>();
        if (request.getUrls() != null && !request.getUrls().isEmpty()) {
            for (Integer attachmentId : request.getUrls()) {
                Attachment attachment = attachmentRepository.findById(attachmentId).orElseThrow();
                attachment.setMessage(message);
                attachments.add(attachment);
            }
            attachmentRepository.saveAll(attachments);
            message.setAttachments(attachments);
        }
        
        List<MessageMention> messageMentions = new ArrayList<>();
        if(request.getMemberIds() != null && !request.getMemberIds().isEmpty()) {
            // Fetch all conversation members in one query to avoid N+1 and auto-flush issues
            List<ConversationMember> mentionedMembers = conversationMemberRepository.findAllById(request.getMemberIds());
            
            for (ConversationMember member : mentionedMembers) {
                MessageMention messageMention = new MessageMention();
                messageMention.setMessage(message);
                messageMention.setConversationMember(member);
                messageMentions.add(messageMention);
            }
            messageMentionRepository.saveAll(messageMentions);
            message.setMentions(messageMentions);
        }
        

        MessageResponse response = toMessageResponseWithInteractions(message);
        webSocketService.sendMessageToConversation(conversation.getId(), response);
    }

    public Page<MessageResponse> getLatestMessages(Integer conversationId, Integer userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Message> messagePage =
                messageRepository.findVisibleChannelMessages(conversationId, userId, pageable);

        return messagePage.map(this::toMessageResponseWithInteractions);
    }


    public Page<MessageResponse> getMessageContext(Integer messageId, Integer userId, int size) {
        Message targetMessage = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        
        Integer conversationId = targetMessage.getConversation().getId();
        
        // Đếm số tin nhắn MỚI HƠN target (vì sort DESC, tin mới nhất ở page 0)
        Integer countAfter = messageRepository.countVisibleMessagesAfter(
                conversationId, userId, targetMessage.getCreatedAt());
        
        // Tính page thực tế chứa message
        Integer page = (Integer) (countAfter / size);
        
        // Gọi lại getLatestMessages với page đã tính
        return getLatestMessages(conversationId, userId, page, size);
    }

    private MessageResponse toMessageResponseWithInteractions(Message message) {
        // Map basic message info
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
                .build();

        // Parent message (reply)
        if (message.getParentMessage() != null) {
            response.setParentMessageId(message.getParentMessage().getId());
            response.setParentContent(message.getParentMessage().getContent());
        }

        // Map reactions - build full URL from filename stored in DB
        if (message.getReactions() != null && !message.getReactions().isEmpty()) {
            List<MessageResponse.ReactionInfo> reactions = message.getReactions().stream()
                    .map(r -> MessageResponse.ReactionInfo.builder()
                            .userId(r.getConversationMember().getUser().getUserId())
                            .userName(r.getConversationMember().getUser().getFullName())
                            .emoji(fileService.buildEmojiUrl(r.getEmoji())) // Build full URL
                            .reactedAt(r.getReactedAt())
                            .build())
                    .toList();
            response.setReactions(reactions);
        }

        // Map mentions
        if (message.getMentions() != null && !message.getMentions().isEmpty()) {
            List<MessageResponse.MentionInfo> mentions = message.getMentions().stream()
                    .map(m -> MessageResponse.MentionInfo.builder()
                            .memberId(m.getConversationMember().getId())
                            .userId(m.getConversationMember().getUser().getUserId())
                            .userName(m.getConversationMember().getUser().getFullName())
                            .build())
                    .toList();
            response.setMentions(mentions);
        }

        // Map attachments
        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            List<MessageResponse.AttachmentInfo> attachments = message.getAttachments().stream()
                    .map(a -> MessageResponse.AttachmentInfo.builder()
                            .id(a.getId())
                            .fileUrl(a.getFileUrl())
                            .fileType(a.getFileType())
                            .fileSize(a.getFileSize())
                            .build())
                    .toList();
            response.setAttachments(attachments);
        }

        // Check if pinned
        Boolean isPinned = pinnedMessageRepository.existsByMessage_Id(message.getId());
        response.setIsPinned(isPinned);
        
        response.setThreadReplyCount(message.getThreadReplyCount());
        if(message.getThread() != null) {

            response.setThreadId(message.getThread().getId());
        }
        return response;
    }


    public void updateMessage(Integer messageId, Integer userId, MessageUpdateRequest  request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
        if(!message.getSender().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }

        message.setContent(request.getMessage());
        message.setUpdatedAt(System.currentTimeMillis());
        messageRepository.save(message);

        // Send WebSocket notification with updated message
        MessageResponse response = toMessageResponseWithInteractions(message);
        webSocketService.sendMessageToConversation(message.getConversation().getId(), response);
    }
    @Transactional
    public void revokeMessage(Integer messageId, Integer userId){
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        if(!message.getSender().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        message.setStatus(MessageStatus.REVOKED);
        message.setUpdatedAt(System.currentTimeMillis());
        messageRepository.save(message);
        
        // Notify all members in conversation that message was revoked
        MessageResponse response = MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .status(MessageStatus.REVOKED)
                .senderId(message.getSender().getUserId())
                .senderName(message.getSender().getFullName())
                .content(null) // Content is hidden when revoked
                .updatedAt(message.getUpdatedAt())
                .build();
        
        webSocketService.sendMessageToConversation(message.getConversation().getId(), response);
    }

    @Transactional
    public void deleteMessageForMe(Integer messageId, Integer userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        // Kiểm tra xem message đã bị ẩn chưa
        if (hiddenMessageRepository.existsByUser_UserIdAndMessage_Id(userId, messageId)) {
            return;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        HiddenMessage hidden = new HiddenMessage();
        hidden.setUser(user);
        hidden.setMessage(message);
        hidden.setHiddenAt(System.currentTimeMillis());
        hiddenMessageRepository.save(hidden);
    }

    @Transactional
    public void replyMessage(Integer messageId, CreateMessageRequest request, Integer conversationId) {

    }

    public Page<MessageResponse> getMessagesInThread(Integer threadId, Integer userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Message> messagePage = messageRepository.findVisibleThreadMessages(threadId, userId, pageable);

        return messagePage.map(this::toMessageResponseWithInteractions);
    }

    public Page<MessageSearchResponse> searchMessages(Integer conversationId, Integer userId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Message> messagePage = messageRepository.searchMessages(conversationId, userId, keyword, pageable);

        return messagePage.map(m -> MessageSearchResponse.builder()
                .id(m.getId())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .senderId(m.getSender().getUserId())
                .senderName(m.getSender().getFullName())
                .senderAvatar(m.getSender().getAvatar())
                .build());
    }
}
