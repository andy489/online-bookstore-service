# Database Connection Guide

The bookstore uses **MySQL 8** — compatible with Sequel Ace.

---

## Connection Details

| Field    | Value       |
|----------|-------------|
| Host     | `localhost` |
| Port     | `3306`      |
| Database | `bookstore` |
| Username | `bookstore` |
| Password | `bookstore` |

> These match the values in `.env` (`DB_USERNAME` / `DB_PASSWORD`).

---

## Sequel Ace (Mac — MySQL client)

1. Open Sequel Ace → **TCP/IP** tab
2. Fill in the details above
3. Leave Database field empty or type `bookstore`
4. Click **Connect**

---

## TablePlus (alternative, also free)

Supports MySQL natively. Download at https://tableplus.com — same connection details.

---

## mysql CLI (terminal)

```bash
mysql -h 127.0.0.1 -P 3306 -u bookstore -p bookstore
# password: bookstore
```

---

## Useful Queries

```sql
-- See all users
SELECT id, email, role, created_at FROM users ORDER BY id;

-- See all books
SELECT id, title, author, price, stock FROM books WHERE deleted = 0 ORDER BY id;

-- See all orders
SELECT id, status, total_price, created_at FROM orders ORDER BY created_at DESC;

-- See cart contents
SELECT u.email, b.title, ci.quantity
FROM cart_items ci
JOIN carts c ON ci.cart_id = c.id
JOIN users u ON c.user_id = u.id
JOIN books b ON ci.book_id = b.id;
```

---

## Notes

- The database only exists while the Docker container is running (`docker compose up db`)
- Data persists in the `mysql_data` Docker volume between restarts
- To wipe everything and reseed: `docker compose down -v && docker compose up --build`
