package com.examportal.repository;

import com.examportal.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TestRepository extends JpaRepository<Test, Long>, JpaSpecificationExecutor<Test> {
    
    List<Test> findByDepartment(String department);
    
    List<Test> findByDepartmentAndStartDateTimeBeforeOrderByStartDateTimeDesc(
        String department, 
        LocalDateTime dateTime
    );
    
    List<Test> findByDepartmentAndEndDateTimeAfterOrderByStartDateTimeAsc(
        String department, 
        LocalDateTime dateTime
    );
}
