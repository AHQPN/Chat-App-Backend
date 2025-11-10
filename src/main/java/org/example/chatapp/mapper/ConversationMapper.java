package org.example.chatapp.mapper;

import org.example.chatapp.dto.request.CreateConversationRequest;
import org.example.chatapp.dto.request.UpdateConversationRequest;
import org.example.chatapp.entity.Conversation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ConversationMapper {
    @Mapping(source = "workspaceId", target = "workspace.id")
    Conversation convertToConversation(CreateConversationRequest request);

    void toExistingCourse(@MappingTarget Conversation course, UpdateConversationRequest request);
}
