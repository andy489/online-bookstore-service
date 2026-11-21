-- Seed users for LOCAL DEVELOPMENT ONLY.
-- WARNING: Do NOT apply this migration to staging or production environments.
-- These accounts use weak passwords ("1234") and are intended for local testing only.
--
-- Passwords are PBKDF2-encoded (Spring Security v5.8 defaults: SHA-256, 310000 iterations)
-- and stored with the {pbkdf2} prefix required by DelegatingPasswordEncoder.
--
-- pesho@gmail.com        / password: 1234 / role: USER
-- rumen@bookstore.local  / password: 1234 / role: ADMIN
-- anya@bookstore.local   / password: 1234 / role: USER

INSERT INTO users (email, password_hash, first_name, last_name, role, created_at, updated_at)
VALUES ('pesho@gmail.com',
        '{pbkdf2}6ba3d44248fb202021ee1970ab46bfaa59c1ea089f43d4b678a7883a64e30fea0dab09cf92094c7000faa228c4907a67',
        'Pesho', 'Peshev', 'USER',
        NOW(), NOW()),

       ('rumen@bookstore.local',
        '{pbkdf2}538e440c083d4ba652e5692ba284e410432a6e670dcf39e73b248e4e0cdea680fc0e81bdd02b9a2a935761f8c3176195',
        'Rumen', 'Rumenov', 'ADMIN',
        NOW(), NOW()),

       ('anya@bookstore.local',
        '{pbkdf2}baaf8a20d68371a636e0871ff002770ab15e689ee663f04c058de17e4e1ee1c5f6a23900f2d1bfeb39647bc09708faf6',
        'Anya', 'Anyova', 'USER',
        NOW(), NOW());
