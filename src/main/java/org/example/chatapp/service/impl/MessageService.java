package org.example.chatapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.CreateMessageRequest;
import org.example.chatapp.dto.request.MessageUpdateRequest;
import org.example.chatapp.dto.response.MessageResponse;
import org.example.chatapp.entity.*;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.mapper.MessageMapper;
import org.example.chatapp.repository.*;
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

    @Transactional
    public void createMessage(CreateMessageRequest request,Integer memberId, Integer conversationId,Integer parentMessageId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        if(!conversationMemberRepository.existsByIdAndConversation_Id(memberId, conversationId))
            throw new AppException(ErrorCode.MEMBER_NOT_FOUND);

        ConversationMember sender = conversationMemberRepository.getConversationMembersById(memberId);
        Message message = messageMapper.convertToMessage(request);
        message.setCreatedAt(System.currentTimeMillis());
        message.setConversation(conversation);
        message.setSender(sender.getUser());

        if (request.getUrls() != null) {
            List<Attachment> attachments = new ArrayList<>();
            request.getUrls().forEach(attachment -> {
               Attachment attachment1 = attachmentRepository.findById(attachment).orElseThrow();
               attachment1.setMessage(message);
               attachments.add(attachment1);

            });
            attachmentRepository.saveAll(attachments);
        }
        if(request.getMemberIds() != null) {
            List<MessageMention> messageMentions = new ArrayList<>();
            request.getMemberIds().forEach(id -> {
                MessageMention messageMention = new MessageMention();
                messageMention.setMessage(message);
                messageMention.setConversationMember(conversationMemberRepository.getConversationMembersById(id));
               messageMentions.add(messageMention);
            });
            messageMentionRepository.saveAll(messageMentions);
        }

        if(parentMessageId != null){
            Message parentMessage = messageRepository.findById(parentMessageId)
                    .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
            message.setParentMessage(parentMessage);
        }
        messageRepository.save(message);
        MessageResponse response = messageMapper.toMessageResponse(message);
        webSocketService.sendMessageToConversation(conversation.getId(), response);
    }

    public Page<MessageResponse> getLatestMessages(Integer conversationId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Message> messagePage =
                messageRepository.findByConversationId(conversationId, pageable);

        return messagePage.map(messageMapper::toMessageResponse);
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

    }
    @Transactional
    public void deleteMessage(Integer messageId, Integer userId){
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));

        if(!message.getSender().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        message.setIsDeleted(true);
        messageRepository.save(message);
    }

    @Transactional
    public void replyMessage(Integer messageId, CreateMessageRequest request, Integer conversationId) {


    }
}
