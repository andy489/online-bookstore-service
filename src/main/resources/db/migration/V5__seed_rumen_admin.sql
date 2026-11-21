-- Seed Rumen as ADMIN for local development.
-- rumen@bookstore.local  / password: 1234  / role: ADMIN

INSERT INTO users (email, password_hash, first_name, last_name, role, created_at, updated_at)
VALUES ('rumen@bookstore.local',
        '538e440c083d4ba652e5692ba284e410432a6e670dcf39e73b248e4e0cdea680fc0e81bdd02b9a2a935761f8c3176195',
        'Rumen', 'Rumenov', 'ADMIN',
        NOW(), NOW());
