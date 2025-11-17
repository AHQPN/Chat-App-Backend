package org.example.chatapp.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MessageInteractionResponse {
    private Integer id;
    private String content;
    private List<ReactionInfo> reactions;
    private List<Integer> mentions;
    private Boolean pinned;
    private List<String> attachments;
    private Long createdAt;

    @AllArgsConstructor
    public static class ReactionInfo {
        private Integer userId;
        private String emoji;
        private Long reactedAt;
    }
}
