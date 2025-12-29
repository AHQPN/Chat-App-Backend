package org.example.chatapp.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateMessageRequest{
    private String content;
    private List<Integer> urls;
    private List<Integer> memberIds;
    private Integer parentMessageId;
    private Integer threadId;
}
