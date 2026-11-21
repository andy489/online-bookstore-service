-- Enforce non-negative stock and positive quantities at the DB level
ALTER TABLE books
    ADD CONSTRAINT chk_books_stock_non_negative CHECK (stock >= 0);

ALTER TABLE cart_items
    ADD CONSTRAINT chk_cart_items_quantity_positive CHECK (quantity > 0);

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_quantity_positive CHECK (quantity > 0);
