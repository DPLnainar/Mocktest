package com.examportal.security;

import com.examportal.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetails implementation
 * 
 * Wraps User entity and provides department-based authorities
 * Format: ROLE_STUDENT, ROLE_MODERATOR, DEPT_ECE, DEPT_CSE
 */
@Data
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private Long id;
    private String email;
    private String password;
    private String fullName;
    private String department;
    private boolean enabled;
    private boolean accountNonLocked;
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Build CustomUserDetails from User entity
     */
    public static CustomUserDetails build(User user) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Add role-based authorities (e.g., ROLE_STUDENT, ROLE_MODERATOR)
        user.getRoles().forEach(role -> 
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()))
        );

        // Add department-based authority (e.g., DEPT_ECE, DEPT_CSE)
        authorities.add(new SimpleGrantedAuthority(user.getDepartmentAuthority()));

        return new CustomUserDetails(
            user.getId(),
            user.getEmail(),
            user.getPassword(),
            user.getFullName(),
            user.getDepartment(),
            user.getEnabled(),
            user.getAccountNonLocked(),
            authorities
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
