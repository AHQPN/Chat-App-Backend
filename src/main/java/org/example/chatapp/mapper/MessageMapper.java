package org.example.chatapp.mapper;

import org.example.chatapp.dto.request.CreateMessageRequest;
import org.example.chatapp.dto.response.MessageResponse;
import org.example.chatapp.dto.response.PinMessageResponse;
import org.example.chatapp.entity.Message;
import org.example.chatapp.entity.PinnedMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MessageMapper {
    Message convertToMessage(CreateMessageRequest request);

    @Mapping(source = "conversation.id", target = "conversationId")
    @Mapping(source = "sender.userId", target = "senderId")
    @Mapping(source = "sender.fullName", target = "senderName")

    @Mapping(source = "parentMessage.id", target = "parentMessageId")
    @Mapping(source = "parentMessage.content", target = "parentContent")
    MessageResponse toMessageResponse(Message message);


    @Mapping(source = "message.id", target = "messageId")
    @Mapping(source = "conversation.id", target = "conversationId")
    @Mapping(source = "pinnedBy.id", target = "pinnedById")
    @Mapping(source = "pinnedBy.user.fullName", target = "pinnedByMember")
    @Mapping(source = "pinnedAt", target = "pinnedAt")
    PinMessageResponse toPinMessage(PinnedMessage pinnedMessage);
}
