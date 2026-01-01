package com.examportal.repository;

import com.examportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByDepartment(String department);

    @Query("SELECT DISTINCT u FROM User u JOIN u.userRoles ur JOIN ur.role r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    @Query("SELECT DISTINCT u FROM User u JOIN u.userRoles ur JOIN ur.role r WHERE r.name = :roleName AND u.department = :department")
    List<User> findByRoleNameAndDepartment(@Param("roleName") String roleName, @Param("department") String department);
}
