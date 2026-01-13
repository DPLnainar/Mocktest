package com.examportal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_questions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private Integer marks;

    @Column(length = 255)
    private String sectionName; // e.g., "Part-I", "Section-A"

    private Integer orderIndex; // To maintain sequence within the test
}
