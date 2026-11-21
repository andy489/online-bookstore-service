-- Seed users for LOCAL DEVELOPMENT ONLY.
-- WARNING: Do NOT apply this migration to staging or production environments.
-- These accounts use weak passwords ("12345678") and are intended for local testing only.
--
-- Passwords are PBKDF2-encoded (Spring Security v5.8 defaults: SHA-256, 310000 iterations)
-- and stored with the {pbkdf2} prefix required by DelegatingPasswordEncoder.
--
-- pesho@gmail.com        / password: 12345678 / role: USER
-- rumen@bookstore.local  / password: 12345678 / role: ADMIN
-- anya@bookstore.local   / password: 12345678 / role: USER

INSERT INTO users (email, password_hash, first_name, last_name, role, created_at, updated_at)
VALUES ('pesho@gmail.com',
        '{pbkdf2}be901c3bccf69edcf20c62ffcff421c69a67c4b6cab6f1f01bac497da6d03b41e554b66f10ab7d5a9af61dc423ce16af',
        'Pesho', 'Peshev', 'USER',
        NOW(), NOW()),

       ('rumen@bookstore.local',
        '{pbkdf2}379e40853e0d18a811f69e442043fd7c125e9b6552e24bc9e1cf42f19cfd33e81a12760e0d88d4b8d7750f32b9dd8a99',
        'Rumen', 'Rumenov', 'ADMIN',
        NOW(), NOW()),

       ('anya@bookstore.local',
        '{pbkdf2}407b178226a86712b723e12254c91a1acb30c4cddb5efe1d2006ce27ea57efdcf8fd1d1118a48402c9df6532d134448c',
        'Anya', 'Anyova', 'USER',
        NOW(), NOW());
