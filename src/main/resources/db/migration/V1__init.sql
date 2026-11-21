CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_users_email ON users (email);

CREATE TABLE categories
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500)
);

CREATE TABLE books
(
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255)   NOT NULL,
    author          VARCHAR(255)   NOT NULL,
    description     TEXT,
    price           NUMERIC(10, 2) NOT NULL,
    stock           INTEGER        NOT NULL,
    cover_image_url VARCHAR(500),
    category_id     BIGINT REFERENCES categories (id) ON DELETE SET NULL,
    deleted         BOOLEAN        NOT NULL DEFAULT FALSE,
    version         BIGINT         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_books_deleted ON books (deleted);
CREATE INDEX idx_books_category_id ON books (category_id);

CREATE TABLE carts
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE cart_items
(
    id       BIGSERIAL PRIMARY KEY,
    cart_id  BIGINT  NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    book_id  BIGINT  NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL,
    UNIQUE (cart_id, book_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);

CREATE TABLE orders
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT         NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    status      VARCHAR(20)    NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL,
    updated_at  TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_orders_user_id ON orders (user_id);

CREATE TABLE order_items
(
    id                BIGSERIAL PRIMARY KEY,
    order_id          BIGINT         NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    book_id           BIGINT         NOT NULL REFERENCES books (id) ON DELETE RESTRICT,
    quantity          INTEGER        NOT NULL,
    price_at_purchase NUMERIC(10, 2) NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
