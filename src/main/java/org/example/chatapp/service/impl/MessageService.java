package org.example.chatapp.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.chatapp.dto.request.CreateMessageRequest;
import org.example.chatapp.dto.request.MessageUpdateRequest;
import org.example.chatapp.dto.response.MessageResponse;
import org.example.chatapp.entity.Conversation;
import org.example.chatapp.entity.ConversationMember;
import org.example.chatapp.entity.Message;
import org.example.chatapp.exception.AppException;
import org.example.chatapp.exception.ErrorCode;
import org.example.chatapp.mapper.MessageMapper;
import org.example.chatapp.repository.ConversationMemberRepository;
import org.example.chatapp.repository.ConversationRepository;
import org.example.chatapp.repository.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.print.Book;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final MessageMapper messageMapper;
    private final WebSocketService webSocketService;

    @Transactional
    public MessageResponse createMessage(CreateMessageRequest request,Integer memberId, Integer conversationId,Integer parentMessageId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        ConversationMember sender = conversationMemberRepository
                .findByConversation_IdAndUser_UserId(conversationId,memberId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_IN_CONVERSATION));

        Message message = messageMapper.convertToMessage(request);
        message.setCreatedAt(System.currentTimeMillis());
        message.setConversation(conversation);
        message.setSender(sender.getUser());
//        if (request.getAttachments() != null) {
//            request.getAttachments().forEach(attachment -> {
//
//
//            });
//        }
        if(parentMessageId!=null){
            Message parentMessage = messageRepository.findById(parentMessageId)
                    .orElseThrow(() -> new AppException(ErrorCode.MESSAGE_NOT_FOUND));
            message.setParentMessage(parentMessage);
        }
        messageRepository.save(message);

        MessageResponse response = messageMapper.toMessageResponse(message);
        webSocketService.sendMessageToConversation(conversation.getId(), response);

        return response;
    }

    public Page<MessageResponse> getMessagesByConversation(Integer conversationId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Message> messagePage = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
        Page<Book> pageTuts;

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
