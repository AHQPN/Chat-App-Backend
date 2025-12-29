package org.example.chatapp.security.services;

import org.example.chatapp.entity.User;
import org.example.chatapp.repository.UserRepository;
import org.example.chatapp.security.model.UserDetailsImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // constructor injection (recommended)
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // transactional read-only
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new UsernameNotFoundException("Identifier cannot be null or empty");
        }

        User user = userRepository.findByPhoneNumberOrEmail(identifier,identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found" ));

        return UserDetailsImpl.build(user);
    }
}
