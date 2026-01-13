DO $$
DECLARE
    u_id BIGINT;
    r_id BIGINT;
BEGIN
    -- Get Role ID
    SELECT id INTO r_id FROM roles WHERE name = 'MODERATOR';

    -- Check if user exists
    SELECT id INTO u_id FROM users WHERE email = 'moderator@examportal.com';

    IF u_id IS NULL THEN
        -- Insert new user
        INSERT INTO users (email, password, first_name, last_name, department, enabled, username, profile)
        VALUES ('moderator@examportal.com', crypt('moderator123', gen_salt('bf', 10)), 'Test', 'Moderator', 'CSE', true, 'moderator', 'MODERATOR')
        RETURNING id INTO u_id;
    ELSE
        -- Update password for existing user
        UPDATE users SET password = crypt('moderator123', gen_salt('bf', 10)) WHERE id = u_id;
    END IF;

    -- Assign role if not exists
    IF NOT EXISTS (SELECT 1 FROM user_role WHERE user_id = u_id AND role_id = r_id) THEN
        INSERT INTO user_role (user_id, role_id) VALUES (u_id, r_id);
    END IF;
END $$;
