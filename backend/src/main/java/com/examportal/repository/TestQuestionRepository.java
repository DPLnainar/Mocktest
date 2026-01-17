package com.examportal.repository;

import com.examportal.entity.TestQuestion;
import com.examportal.entity.TestQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TestQuestionRepository extends JpaRepository<TestQuestion, TestQuestionId> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM TestQuestion tq WHERE tq.test.id = :testId")
    void deleteByTestId(@Param("testId") Long testId);
}
