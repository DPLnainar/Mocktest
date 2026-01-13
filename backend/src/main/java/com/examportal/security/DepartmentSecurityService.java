package com.examportal.security;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Department Security Service
 * 
 * Provides row-level security by filtering queries based on user's department
 * Ensures ECE moderators cannot see CSE data
 * 
 * Usage:
 * Specification<Exam> spec = departmentSecurityService.hasDepartmentAccess();
 * examRepository.findAll(spec);
 */
@Service
public class DepartmentSecurityService {

    public DepartmentSecurityService() {
    }

    /**
     * Get current authenticated user's department
     */
    public String getCurrentUserDepartment() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getDepartment();
        }

        throw new IllegalStateException("No authenticated user found");
    }

    /**
     * Get current authenticated user's ID
     */
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }

        throw new IllegalStateException("No authenticated user found");
    }

    /**
     * Check if current user is admin (bypass department restrictions)
     */
    public boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ADMIN"));
        }

        return false;
    }

    /**
     * Get department filter specification
     * Admins bypass this filter
     */
    public <T> Specification<T> hasDepartmentAccess(String departmentField) {
        return (root, query, criteriaBuilder) -> {
            if (isCurrentUserAdmin()) {
                return criteriaBuilder.conjunction(); // No restriction for admins
            }

            String userDepartment = getCurrentUserDepartment();
            return criteriaBuilder.equal(root.get(departmentField), userDepartment);
        };
    }

    /**
     * Verify user has access to specific department
     * Throws exception if access denied
     */
    public void verifyDepartmentAccess(String targetDepartment) {
        if (isCurrentUserAdmin()) {
            return; // Admins have access to all departments
        }

        String userDepartment = getCurrentUserDepartment();
        if (!userDepartment.equalsIgnoreCase(targetDepartment)) {
            throw new SecurityException(
                    String.format("Access denied: User from %s cannot access %s data",
                            userDepartment, targetDepartment));
        }
    }
}
