package com.examportal.security;

import com.examportal.entity.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collection;
import java.util.stream.Collectors;

@Data
public class CustomUserDetails implements UserDetails {

    private User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    // --- DELEGATION METHODS (These were missing!) ---
    
    public Long getId() {
        return user.getId();
    }
    
    public String getEmail() {
        return user.getEmail();
    }

    public String getFirstName() {
        return user.getFirstName();
    }

    public String getLastName() {
        return user.getLastName();
    }

    public String getDepartment() {
        // Assuming 'profile' field stores department or similar info
        return user.getProfile(); 
    }

    // --- UserDetails Implementation ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.user.getUserRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRole().getRoleName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return user.isEnabled(); }
}
