package com.examportal.controller;

import com.examportal.dto.JwtResponse;
import com.examportal.dto.LoginRequest;
import com.examportal.dto.RegisterRequest;
import com.examportal.entity.Role;
import com.examportal.entity.User;
import com.examportal.entity.UserRole;
import com.examportal.repository.RoleRepository;
import com.examportal.repository.UserRepository;
import com.examportal.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authentication Controller
 * 
 * Endpoints:
 * - POST /api/auth/login - Login and get JWT token
 * - POST /api/auth/register - Register new user
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    /**
     * Login endpoint
     * Returns JWT token with department and role claims
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        System.out.println("LOGIN ATTEMPT: " + loginRequest.getEmail());
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(),
                loginRequest.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        com.examportal.security.CustomUserDetails userDetails = 
            (com.examportal.security.CustomUserDetails) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());

        return ResponseEntity.ok(JwtResponse.builder()
            .token(jwt)
            .userId(userDetails.getId())
            .email(userDetails.getEmail())
            .fullName(userDetails.getFullName())
            .department(userDetails.getDepartment())
            .roles(roles)
            .build());
    }

    /**
     * Register new user
     * Default role: STUDENT
     * Moderators can only be created by admins
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Error: Email is already in use!");
        }

        // Get role from repository
        Role role = roleRepository.findByName(registerRequest.getRole())
            .orElseGet(() -> roleRepository.findByName(Role.STUDENT)
                .orElseThrow(() -> new RuntimeException("Error: Role not found")));

        // Create new user
        String[] nameParts = registerRequest.getFullName().split(" ", 2);
        User user = User.builder()
            .username(registerRequest.getEmail().split("@")[0])
            .email(registerRequest.getEmail())
            .password(passwordEncoder.encode(registerRequest.getPassword()))
            .firstName(nameParts.length > 0 ? nameParts[0] : "")
            .lastName(nameParts.length > 1 ? nameParts[1] : "")
            .profile(registerRequest.getDepartment().toUpperCase())
            .enabled(true)
            .build();

        // Create UserRole relation
        Set<UserRole> userRoles = new HashSet<>();
        UserRole userRoleRelation = new UserRole();
        userRoleRelation.setRole(role);
        userRoleRelation.setUser(user);
        userRoles.add(userRoleRelation);
        
        user.setUserRoles(userRoles);

        userRepository.save(user);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body("User registered successfully!");
    }

    /**
     * Get current user info
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        com.examportal.security.CustomUserDetails userDetails = 
            (com.examportal.security.CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(userDetails);
    }
}
