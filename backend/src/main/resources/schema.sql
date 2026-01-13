-- Initial Database Schema
-- This file documents the schema created by JPA/Hibernate

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    roll_number VARCHAR(20),
    enabled BOOLEAN DEFAULT true,
    account_non_locked BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
);

-- User-Role mapping
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_department ON users(department);

-- Insert default roles
INSERT INTO roles (name, description) VALUES 
    ('STUDENT', 'Student role - can take exams and view own submissions'),
    ('MODERATOR', 'Moderator role - can create and monitor exams (department-restricted)'),
    ('ADMIN', 'Admin role - full system access')
ON CONFLICT (name) DO NOTHING;

-- Default admin user is handled by DatabaseInitializer.java to ensure correct BCrypt encoding

COMMENT ON TABLE users IS 'All system users: students, moderators, admins';
COMMENT ON TABLE roles IS 'Role hierarchy: STUDENT < MODERATOR < ADMIN';
COMMENT ON COLUMN users.department IS 'Department for row-level security (ECE, CSE, MECH, etc.)';
