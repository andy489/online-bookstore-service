-- Seed Anya as USER for local development.
-- anya@bookstore.local  / password: 1234  / role: USER

INSERT INTO users (email, password_hash, first_name, last_name, role, created_at, updated_at)
VALUES ('anya@bookstore.local',
        'baaf8a20d68371a636e0871ff002770ab15e689ee663f04c058de17e4e1ee1c5f6a23900f2d1bfeb39647bc09708faf6',
        'Anya', 'Anyova', 'USER',
        NOW(), NOW());
