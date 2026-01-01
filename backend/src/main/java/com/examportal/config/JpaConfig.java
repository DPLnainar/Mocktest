package com.examportal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Phase 10: JPA Configuration
 * 
 * Enables auditing for @CreatedDate, @LastModifiedDate
 * Configures batch operations and query optimizations
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    
    // Batch operations are configured in application-optimization.yml
    // hibernate.jdbc.batch_size=20
    // hibernate.order_inserts=true
    // hibernate.order_updates=true
}
