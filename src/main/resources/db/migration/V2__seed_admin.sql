-- Seed users for local development.
-- Passwords are PBKDF2-encoded (Spring Security v5.8 defaults: SHA-256, 310000 iterations).
--
-- pesho@gmail.com  / password: 1234  / role: USER
-- admin@bookstore.local / see instructions below for ADMIN

INSERT INTO users (email, password_hash, first_name, last_name, role, created_at, updated_at)
VALUES ('pesho@gmail.com',
        '6ba3d44248fb202021ee1970ab46bfaa59c1ea089f43d4b678a7883a64e30fea0dab09cf92094c7000faa228c4907a67',
        'Pesho', 'Peshev', 'USER',
        NOW(), NOW());

-- To create an admin, register via POST /api/auth/register then promote:
--   UPDATE users SET role = 'ADMIN' WHERE email = 'admin@bookstore.local';
