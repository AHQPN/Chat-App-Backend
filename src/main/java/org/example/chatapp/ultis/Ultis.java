package org.example.chatapp.ultis;

import org.example.chatapp.security.model.UserDetailsImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;

public class Ultis {
    public static Integer extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            Object userDetails = ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
            if (userDetails instanceof UserDetailsImpl) {
                return ((UserDetailsImpl) userDetails).getId();
            }
        }
        return null;
    }
}
