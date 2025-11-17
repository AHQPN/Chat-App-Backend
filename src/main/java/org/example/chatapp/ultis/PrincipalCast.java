package org.example.chatapp.ultis;

import org.example.chatapp.security.model.UserDetailsImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.security.Principal;

public class PrincipalCast {

    public static Integer castUserIdFromPrincipal(Principal principal)
    {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        UserDetailsImpl user = (UserDetailsImpl) auth.getPrincipal();
        return user.getId();
    }
}
