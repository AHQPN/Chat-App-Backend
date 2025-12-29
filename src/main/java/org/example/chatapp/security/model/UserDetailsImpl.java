package org.example.chatapp.security.model;

import org.example.chatapp.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    @Getter
    private final Integer id;
    @Getter
    private final String fullName;
    private final String password;
    private final String phoneNumber;
    @Getter
    private final String email;
    @Getter
    private final String role;
    @Getter
    private final String avatar;
    private final Collection<? extends GrantedAuthority> authorities;
    @Getter
    private final boolean isVerified;


    // build từ entity User
    public static UserDetailsImpl build(User user) {
        String roleName = "";
        if (user.getUserType() != null) roleName += user.getUserType().name();
        else roleName += "User";

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + roleName.toUpperCase());

        return new UserDetailsImpl(
                user.getUserId(),
                user.getFullName(),
                user.getPasswordHash(),
                user.getPhoneNumber(),
                user.getEmail(),
                roleName,
                user.getAvatar(),
                Collections.singletonList(authority)
                ,user.getIsVerified()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return phoneNumber != null ? phoneNumber : email; // Spring Security dùng đây làm username
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDetailsImpl)) return false;
        UserDetailsImpl that = (UserDetailsImpl) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
