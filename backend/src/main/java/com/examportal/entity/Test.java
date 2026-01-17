package com.examportal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String department;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TestStatus status = TestStatus.DRAFT;

    @Column(length = 100)
    private String testType; // "Placement Drive", "Practice", "Contest"

    @Column(length = 5000)
    private String instructions; // Rich text instructions for students

    private Long createdBy; // User ID of moderator who created

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TestQuestion> testQuestions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addQuestion(Question question, Integer marks, String sectionName, Integer orderIndex) {
        TestQuestion tq = TestQuestion.builder()
                .id(new TestQuestionId(this.id, question.getId()))
                .test(this)
                .question(question)
                .marks(marks)
                .sectionName(sectionName)
                .orderIndex(orderIndex)
                .build();
        testQuestions.add(tq);
    }
}
