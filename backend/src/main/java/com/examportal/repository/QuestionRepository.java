package com.examportal.repository;

import com.examportal.entity.Question;
import com.examportal.entity.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByDepartment(String department);

    List<Question> findByDepartmentAndType(String department, QuestionType type);

    List<Question> findByIdIn(List<Long> ids);
}
