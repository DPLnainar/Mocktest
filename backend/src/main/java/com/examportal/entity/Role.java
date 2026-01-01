package com.examportal.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Role Entity
 * 
 * Hierarchical roles:
 * - STUDENT: Can take exams, view own submissions
 * - MODERATOR: Can create exams, monitor students (department-restricted)
 * - ADMIN: Full system access
 */
@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name; // STUDENT, MODERATOR, ADMIN

    @Column(length = 255)
    private String description;

    public static final String STUDENT = "STUDENT";
    public static final String MODERATOR = "MODERATOR";
    public static final String ADMIN = "ADMIN";
}
