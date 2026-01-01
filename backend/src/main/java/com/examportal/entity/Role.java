package com.examportal.entity;

import jakarta.persistence.*;

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

    public Role() {}

    public Role(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
