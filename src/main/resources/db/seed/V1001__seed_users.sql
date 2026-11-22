-- Seed users for LOCAL DEVELOPMENT ONLY.
-- WARNING: Do NOT apply this migration to staging or production environments.
-- Passwords are bcrypt-encoded (Spring Security DelegatingPasswordEncoder default).
--
-- pesho@bookstore.local  / password: 12345678 / role: USER
-- rumen@bookstore.local  / password: 12345678 / role: ADMIN
-- anya@bookstore.local   / password: 12345678 / role: USER

INSERT INTO users (email, password_hash, first_name, last_name, role, created_at, updated_at)
VALUES ('pesho@bookstore.local',
        '{bcrypt}$2a$10$g/nUeg0C2qopLn7gHVuDv.0.daobr5uY0vXsutU64MEBDHnUHt2Gu',
        'Pesho', 'Peshev', 'USER',
        NOW(), NOW()),

       ('rumen@bookstore.local',
        '{bcrypt}$2a$10$u30D8iki7GpkoclZfqTS/.vlKQlmOECN/8l5HiEvDjXPOKlsQ.MyG',
        'Rumen', 'Rumenov', 'ADMIN',
        NOW(), NOW()),

       ('anya@bookstore.local',
        '{bcrypt}$2a$10$SteVIuLZSWwmMnxNk5ABTuWRrXCJFHfkZ6oPAdjskLFjO0qpBrj7C',
        'Anya', 'Anyova', 'USER',
        NOW(), NOW());
