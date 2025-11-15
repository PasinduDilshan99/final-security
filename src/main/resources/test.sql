USE final_security;

-- Create roles table
CREATE TABLE roles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Create privileges table
CREATE TABLE privileges (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Create role_privileges junction table
CREATE TABLE role_privileges (
    role_id INT,
    privilege_id INT,
    PRIMARY KEY (role_id, privilege_id),
    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (privilege_id) REFERENCES privileges(id)
);

-- Create user_roles table
CREATE TABLE user_roles (
    user_id INT,
    role_id INT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Create refresh_tokens table
CREATE TABLE refresh_tokens (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Insert default roles
INSERT INTO roles (id, name) VALUES 
(1, 'ROLE_USER'),
(2, 'ROLE_ADMIN'),
(3, 'ROLE_MODERATOR');

-- Insert default privileges
INSERT INTO privileges (id, name) VALUES 
(1, 'READ_PRIVILEGE'),
(2, 'WRITE_PRIVILEGE'),
(3, 'DELETE_PRIVILEGE'),
(4, 'USER_MANAGEMENT'),
(5, 'ADMIN_ACCESS');

-- Assign privileges to roles
INSERT INTO role_privileges (role_id, privilege_id) VALUES 
(1, 1), -- USER can READ
(2, 1), -- ADMIN can READ
(2, 2), -- ADMIN can WRITE
(2, 3), -- ADMIN can DELETE
(2, 4), -- ADMIN can manage users
(2, 5), -- ADMIN has admin access
(3, 1), -- MODERATOR can READ
(3, 2), -- MODERATOR can WRITE
(3, 4); -- MODERATOR can manage users

-- Update users table structure if needed
ALTER TABLE users MODIFY COLUMN id INT AUTO_INCREMENT;

-- Insert sample users with encoded passwords
-- Password for all users: 'password'
INSERT INTO users (username, password) VALUES 
('user@example.com', '$2a$12$K1gG.8bwI8C1Q8Q1Q1Q1QO.1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q'),
('admin@example.com', '$2a$12$K1gG.8bwI8C1Q8Q1Q1Q1QO.1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q'),
('moderator@example.com', '$2a$12$K1gG.8bwI8C1Q8Q1Q1Q1QO.1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q1Q');

-- Assign roles to users
INSERT INTO user_roles (user_id, role_id) VALUES 
(1, 1), -- user has ROLE_USER
(2, 2), -- admin has ROLE_ADMIN
(3, 3); -- moderator has ROLE_MODERATOR