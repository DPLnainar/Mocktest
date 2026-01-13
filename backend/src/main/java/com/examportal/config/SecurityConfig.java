package com.examportal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.examportal.security.CustomUserDetailsService;
import com.examportal.security.JwtAuthenticationFilter;
import com.examportal.security.JwtAuthenticationEntryPoint;

import java.util.Arrays;

/**
 * Spring Security Configuration
 * 
 * Features:
 * - JWT-based stateless authentication
 * - Method-level security with @PreAuthorize
 * - Role-based access control (STUDENT, MODERATOR, ADMIN)
 * - Department-level authorities (DEPT_ECE, DEPT_CSE)
 * - CORS configuration for frontend
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JwtAuthenticationEntryPoint unauthorizedHandler) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Explicitly permit OPTIONS for CORS
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

                        // Public endpoints with explicit AntPathRequestMatcher
                        .requestMatchers(
                                new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/api/auth/**"))
                        .permitAll()
                        .requestMatchers("/ws/**").permitAll() // WebSocket endpoint
                        .requestMatchers("/actuator/**").permitAll() // Monitoring
                        .requestMatchers("/api/judge0/callback/**").permitAll() // Judge0 webhooks

                        // Student endpoints
                        .requestMatchers("/api/exam/student/**").hasAuthority("STUDENT")
                        .requestMatchers("/api/student/**").hasAuthority("STUDENT")
                        // .requestMatchers("/api/proctor/violation").hasAuthority("STUDENT") // Covered
                        // by new controller
                        // .requestMatchers("/api/proctor/recording/upload").hasAuthority("STUDENT")
                        // /recording/**

                        // Moderator endpoints (department-restricted via @PreAuthorize)
                        .requestMatchers("/api/exam/moderator/**").hasAnyAuthority("MODERATOR", "ADMIN")
                        .requestMatchers("/api/monitoring/**").hasAnyAuthority("MODERATOR", "ADMIN")
                        .requestMatchers("/api/tests/**").permitAll() // DEBUG: Isolate URL vs Method security
                        .requestMatchers("/api/questions/**").hasAnyAuthority("MODERATOR", "ADMIN")
                        .requestMatchers("/api/proctor/attempts/**").hasAnyAuthority("MODERATOR", "ADMIN")
                        .requestMatchers("/api/proctor/recording/**").hasAnyAuthority("MODERATOR", "ADMIN") // Wildcard
                                                                                                            // for
                                                                                                            // viewing
                        .requestMatchers("/api/analytics/**").hasAnyAuthority("MODERATOR", "ADMIN")
                        .requestMatchers("/api/code/verify").authenticated() // Allow all authenticated users to verify
                                                                             // code

                        // Admin endpoints
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        // All other endpoints require authentication
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
