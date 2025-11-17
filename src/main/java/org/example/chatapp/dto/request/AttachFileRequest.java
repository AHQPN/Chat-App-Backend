package org.example.chatapp.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AttachFileRequest {
    private Integer messageId;      // id message muốn đính kèm file
    private List<String> fileUrl;
}
