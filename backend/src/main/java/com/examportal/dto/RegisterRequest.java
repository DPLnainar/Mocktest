package com.examportal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration Request DTO
 */
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Department is required")
    private String department; // ECE, CSE, MECH, etc.

    private String rollNumber; // Optional for students

    @NotBlank(message = "Role is required")
    private String role; // STUDENT, MODERATOR

    public RegisterRequest() {}

    public RegisterRequest(String email, String password, String fullName, String department, String rollNumber, String role) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.department = department;
        this.rollNumber = rollNumber;
        this.role = role;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getRollNumber() { return rollNumber; }
    public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
