UPDATE users SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyi.Xy5MFo3UdSOtExH0Q8GQz7fPyV9J6y' WHERE email = 'admin@examportal.com';
SELECT email, LENGTH(password) as password_length FROM users WHERE email = 'admin@examportal.com';
