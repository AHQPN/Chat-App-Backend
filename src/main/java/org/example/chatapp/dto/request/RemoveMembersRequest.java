package org.example.chatapp.dto.request;

import lombok.Data;
import java.util.Set;

@Data
public class RemoveMembersRequest {
    private Set<Integer> userIds;
}
