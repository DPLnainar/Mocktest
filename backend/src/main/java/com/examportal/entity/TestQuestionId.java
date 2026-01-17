package com.examportal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestQuestionId implements Serializable {

    @Column(name = "test_id")
    private Long testId;

    @Column(name = "question_id")
    private Long questionId;
}
