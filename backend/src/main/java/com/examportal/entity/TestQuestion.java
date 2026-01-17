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

    @EmbeddedId
    private TestQuestionId id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("testId")
    @JoinColumn(name = "test_id", nullable = false)
    private Test test;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private Integer marks;

    @Column(length = 255)
    private String sectionName;

    private Integer orderIndex;
}
