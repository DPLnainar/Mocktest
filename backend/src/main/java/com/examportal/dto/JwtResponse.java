package com.examportal.dto;

import java.util.List;

/**
 * JWT Authentication Response
 */
public class JwtResponse {

    private String token;
    private String type = "Bearer";
    private Long userId;
    private String email;
    private String fullName;
    private String department;
    private List<String> roles;

    public JwtResponse() {}

    public JwtResponse(String token, String type, Long userId, String email, String fullName, String department, List<String> roles) {
        this.token = token;
        this.type = type;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.department = department;
        this.roles = roles;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
