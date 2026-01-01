package com.examportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * JWT Authentication Response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private String token;
    @Builder.Default
    private String type = "Bearer";
    private Long userId;
    private String email;
    private String fullName;
    private String department;
    private List<String> roles;
}
