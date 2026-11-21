-- Add optimistic locking to orders table
ALTER TABLE orders
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Restrict role column to known values
ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'));

-- Restrict order status column to known values
ALTER TABLE orders
    ADD CONSTRAINT chk_orders_status CHECK (status IN ('PENDING', 'SHIPPED', 'DELIVERED', 'CANCELLED'));
