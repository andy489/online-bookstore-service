# Happy-Path Manual Test Guide

A complete walkthrough covering every implemented feature in order. Follow the steps top to bottom — each section builds on the previous one.

**Base URL:** `http://localhost:8080`

**Tip:** Swagger UI is available at `http://localhost:8080/swagger-ui.html` — you can run all requests there instead of curl.

---

## 0. Prerequisites — Start the App

```bash
open -a Docker                        # launch Docker Desktop (macOS) if not already running
docker compose down -v                # wipe volumes so Flyway starts clean
docker compose up --build             # starts MySQL, Redis, and the app
```

`open -a Docker` is macOS shorthand for launching Docker Desktop — equivalent to clicking it in Finder. It starts the Docker daemon, which listens on a Unix socket (`/var/run/docker.sock`). Both `docker compose` and Testcontainers (used by the automated test suite) need this socket to be reachable before they can start containers.

`docker compose down -v` removes the MySQL and Redis volumes. Skipping this on a re-run causes Flyway to fail with "non-empty schema but no history table" because the DB already has tables from a previous run.

Wait until you see `Started BookstoreApplication` in the logs.

Verify the app is up:
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/categories
# Expected: 200
```

---

## 1. Register a New User

```bash
export USER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"alex@test.com","password":"password123","firstName":"Alex","lastName":"Test"}' | jq -r '.token')
echo $USER_TOKEN
```

Expected: a JWT printed to the terminal (starts with `eyJ`). The token is now in `$USER_TOKEN` for all subsequent steps.

---

## 2. Rate Limiting — Exhaust the Login Limit

The login endpoint allows **5 requests per IP per minute**. Trigger the limit to verify it works:

```bash
for i in 1 2 3 4 5 6; do
  echo -n "Attempt $i: "
  curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"wrong@test.com","password":"wrong"}'
  echo
done
```

Expected: attempts 1–5 return `401` (wrong credentials but request reached the handler). Attempt **6** returns `429`:
```json
{"status": 429, "message": "Too many requests — try again in 60 seconds"}
```
with a `Retry-After: 60` response header.

> The rate limit is per-IP per path. Using `/api/auth/login` for the next steps is unaffected once 60 seconds pass, or just proceed — register is a separate bucket.

---

## 3. Login as the Registered User

```bash
export USER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alex@test.com","password":"password123"}' | jq -r '.token')
echo $USER_TOKEN
```

Expected `200` with a fresh token exported to `$USER_TOKEN`.

---

## 4. Get Own Profile

```bash
curl -s http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `200`:
```json
{
  "id": 4,
  "email": "alex@test.com",
  "firstName": "Alex",
  "lastName": "Test",
  "role": "USER"
}
```

> ID 4 because seeded users occupy IDs 1–3 (pesho, rumen, anya).

---

## 5. Update Profile

```bash
curl -s -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Alexander","lastName":"Testov"}' | jq
```

Expected `200` with updated `firstName` and `lastName`.

---

## 6. Change Password

```bash
curl -s -X PATCH http://localhost:8080/api/users/me/password \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"currentPassword":"password123","newPassword":"newpass456"}' | jq
```

Expected `204` (no body). The password change **immediately blocklists** `$USER_TOKEN` — that token is no longer valid even though it hasn't expired. Fetch a fresh token with the new password:

```bash
export USER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alex@test.com","password":"newpass456"}' | jq -r '.token')
echo $USER_TOKEN
```

---

## 7. Browse the Book Catalog (Redis Cache Test)

### First call — hits the database, response is cached

```bash
curl -s "http://localhost:8080/api/books?page=0&size=5&sortBy=title&sortDir=asc" \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `200` with a `PageResponse`:
```json
{
  "content": [
    {
      "id": 1,
      "title": "...",
      "author": "...",
      "price": 12.99,
      "stock": 35,
      "coverImageUrl": "https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/..."
    }
  ],
  "page": 0,
  "size": 5,
  "totalElements": 23,
  "totalPages": 5
}
```

### Second call — served from Redis (no DB hit)

```bash
curl -s "http://localhost:8080/api/books?page=0&size=5&sortBy=title&sortDir=asc" \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Identical response returned instantly from Redis. Cache TTL is 5 minutes.

### Filter by search query

```bash
curl -s "http://localhost:8080/api/books?query=dostoevsky" \
  -H "Authorization: Bearer $USER_TOKEN" | jq '.content[] | {title, author}'
```

Expected: The Brothers Karamazov, Crime and Punishment, The Idiot.

### Filter by category

```bash
# First get category IDs
curl -s http://localhost:8080/api/categories | jq '.[] | {id, name}'

# Then filter by Crime & Mystery category ID (e.g. 3)
curl -s "http://localhost:8080/api/books?categoryId=3" \
  -H "Authorization: Bearer $USER_TOKEN" | jq '.content[] | .title'
```

### Filter by price range

```bash
curl -s "http://localhost:8080/api/books?minPrice=10&maxPrice=12" \
  -H "Authorization: Bearer $USER_TOKEN" | jq '.content[] | {title, price}'
```

Expected: only books priced between $10.00 and $12.00.

### Get a single book by ID

```bash
curl -s http://localhost:8080/api/books/1 \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `200` with full book details including Cloudinary `coverImageUrl`.

---

## 8. Add Items to Cart

> Seeded books have IDs 1–23 (insertion order). Books 1 and 2 are both Dostoevsky (The Brothers Karamazov and Crime and Punishment). Check the catalog from step 7 to confirm IDs if needed.

### View empty cart

```bash
curl -s http://localhost:8080/api/cart \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `200`: `{"items": [], "total": 0.00}` — a cart is auto-created per user on first access.

### Add book 1 (quantity 2)

```bash
curl -s -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookId":1,"quantity":2}' | jq
```

Expected `200` with cart containing one item, quantity 2, and a computed total.

### Add book 2 (quantity 1)

```bash
curl -s -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookId":2,"quantity":1}' | jq
```

Expected `200` with two items in cart.

### Add book 1 again — quantity increments, not duplicated

```bash
curl -s -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookId":1,"quantity":1}' | jq
```

Expected `200`: book 1 quantity is now **3** (not a second row).

### Update item quantity (by book ID, not cart item ID)

```bash
curl -s -X PUT http://localhost:8080/api/cart/items/1 \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"quantity":2}' | jq
```

Expected `200`: book 1 quantity updated to 2. Cart now has 2 items totalling 3 units.

### Remove one item

```bash
curl -s -X DELETE http://localhost:8080/api/cart/items/2 \
  -H "Authorization: Bearer $USER_TOKEN"
```

Expected `204`. Cart now has only book 1.

### Add book 2 back for the order

```bash
curl -s -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookId":2,"quantity":1}' | jq
```

---

## 9. Place an Order

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `201`:
```json
{
  "id": 1,
  "status": "PENDING",
  "totalPrice": 29.97,
  "items": [
    {"bookTitle": "The Brothers Karamazov", "quantity": 2, "priceAtPurchase": 14.99},
    {"bookTitle": "Crime and Punishment",   "quantity": 1, "priceAtPurchase": 12.99}
  ],
  "adjustments": []
}
```

- `priceAtPurchase` is a **snapshot** — book price changes after this point do not affect the order.
- `adjustments` is empty when all items were fully fulfilled.

### Cart is cleared after order

```bash
curl -s http://localhost:8080/api/cart \
  -H "Authorization: Bearer $USER_TOKEN" | jq '.items | length'
```

Expected: `0`

### Placing a second order with empty cart — should fail

```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `400`: `{"message": "Cannot place order with an empty cart"}`

---

## 10. Partial Fulfillment — Order Adjustments

This tests what happens when stock is insufficient for the requested quantity.

First, add a book with low stock to the cart. Book 4 (Tai-Pan) has 20 in stock — we'll request 25:

```bash
curl -s -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"bookId":4,"quantity":25}' | jq
```

Place the order:
```bash
curl -s -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `201` with a non-empty `adjustments` array:
```json
{
  "status": "PENDING",
  "items": [
    {"bookTitle": "Tai-Pan", "quantity": 20, "priceAtPurchase": 15.99}
  ],
  "adjustments": [
    {
      "bookTitle": "Tai-Pan",
      "requested": 25,
      "fulfilled": 20,
      "reason": "Only 20 in stock"
    }
  ]
}
```

The order is placed for what was available. `adjustments` tells the frontend exactly what changed so it can show a confirmation message like "We could only fulfill 20 of your 25 requested copies."

---

## 11. View Order History

### My orders

```bash
curl -s http://localhost:8080/api/orders/my \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `200`: array with all orders placed above, newest first. Each has `status: "PENDING"`.

### Get a specific order by ID

```bash
curl -s http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `200` with full order details.

### Try to access another user's order — should fail

```bash
# Pesho's orders (if any) would have different IDs — try a non-existent or other-user order
curl -s http://localhost:8080/api/orders/999 \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `404` — users can only see their own orders.

---

## 12. Logout — JWT Blocklist

```bash
curl -s -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $USER_TOKEN"
```

Expected `204`. The token's `jti` UUID is now stored in Redis with TTL = remaining token lifetime.

### Verify the token is blocked

```bash
curl -s http://localhost:8080/api/users/me \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `401` — the token is cryptographically valid but its `jti` is on the Redis blocklist, so the request is treated as unauthenticated.

### Log back in with a fresh token

```bash
export USER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alex@test.com","password":"newpass456"}' | jq -r '.token')
```

The new token has a new `jti` and is not blocked.

---

## 13. Admin Flow — Login as Rumen

Rumen is a seeded ADMIN (password `12345678`).

```bash
export ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"rumen@bookstore.local","password":"12345678"}' | jq -r '.token')
echo $ADMIN_TOKEN
```

---

## 14. Admin — List All Orders (Paginated)

```bash
curl -s "http://localhost:8080/api/orders?page=0&size=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

Expected `200` as a `PageResponse`:
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

All orders across all users are returned (including Alex's orders from above).

### Try as regular user — should fail

```bash
curl -s http://localhost:8080/api/orders \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `403`.

---

## 15. Admin — Update Order Status

```bash
curl -s -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"SHIPPED"}' | jq
```

Expected `200` with `"status": "SHIPPED"`.

```bash
curl -s -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"DELIVERED"}' | jq
```

Expected `200` with `"status": "DELIVERED"`.

### Cancel and verify terminal state

```bash
curl -s -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"CANCELLED"}' | jq
```

Expected `200` with `"status": "CANCELLED"`.

```bash
# Try to transition out of CANCELLED — should fail
curl -s -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"SHIPPED"}' | jq
```

Expected `400`: `{"message": "Cannot transition order from CANCELLED to SHIPPED"}`

---

## 16. Admin — Create a New Category

```bash
curl -s -X POST http://localhost:8080/api/categories \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Category","description":"For testing purposes"}' | jq
```

Expected `201` with the new category and its `id`. Save the ID:
```bash
export CAT_ID=7   # use the actual id from the response
```

### List all categories (public — no token needed)

```bash
curl -s http://localhost:8080/api/categories | jq '.[].name'
```

Expected: all 6 seeded categories plus `"Test Category"`.

---

## 17. Admin — Create a New Book

```bash
curl -s -X POST http://localhost:8080/api/books \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\":\"Test Book\",
    \"author\":\"Test Author\",
    \"description\":\"A book created during manual testing.\",
    \"price\":9.99,
    \"stock\":10,
    \"categoryId\":$CAT_ID
  }" | jq
```

Expected `201`. Save the ID:
```bash
export BOOK_ID=24   # use the actual id from the response
```

Note: `coverImageUrl` is `null` until a cover is uploaded.

---

## 18. Admin — Upload a Book Cover (Cloudinary)

Download any small JPEG to test with:
```bash
curl -s -o /tmp/test-cover.jpg \
  "https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/dune.jpg"
```

Upload it as the cover for the new book:
```bash
curl -s -X POST "http://localhost:8080/api/books/$BOOK_ID/cover" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "file=@/tmp/test-cover.jpg" | jq
```

Expected `200` with a Cloudinary `coverImageUrl`:
```json
{
  "id": 24,
  "title": "Test Book",
  "coverImageUrl": "https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/..."
}
```

The Redis book cache is evicted — the next `GET /api/books/24` fetches from DB and re-caches the CDN URL.

---

## 19. Admin — Update a Book

```bash
curl -s -X PUT "http://localhost:8080/api/books/$BOOK_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"title\":\"Updated Test Book\",
    \"author\":\"Test Author\",
    \"description\":\"Updated description.\",
    \"price\":11.99,
    \"stock\":20,
    \"categoryId\":$CAT_ID
  }" | jq
```

Expected `200` with the updated fields.

---

## 20. Admin — Soft-Delete a Book

```bash
curl -s -X DELETE "http://localhost:8080/api/books/$BOOK_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected `204`.

### Verify it's invisible

```bash
curl -s "http://localhost:8080/api/books/$BOOK_ID" \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Expected `404`. Soft-deleted books are excluded from all queries. The book record still exists in the database with `deleted = true`.

---

## 21. Access Denied Checks

Two kinds of denial: **`401`** when there is no valid authentication (missing,
invalid, or blocklisted token), and **`403`** when a valid USER is authenticated
but lacks the ADMIN role.

```bash
# No token on a protected endpoint → 401 (unauthenticated)
curl -s http://localhost:8080/api/users/me | jq

# USER trying to create a book → 403 (authenticated, not admin)
curl -s -X POST http://localhost:8080/api/books \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Hack","author":"Me","description":"x","price":1.00,"stock":1,"categoryId":1}' | jq

# USER trying to delete a category → 403
curl -s -X DELETE http://localhost:8080/api/categories/1 \
  -H "Authorization: Bearer $USER_TOKEN" | jq

# USER trying to update an order status → 403
curl -s -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"status":"SHIPPED"}' | jq

# USER trying to access admin order list → 403
curl -s http://localhost:8080/api/orders \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

---

## 22. Validation Error Checks

```bash
# Register with invalid email format
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"not-an-email","password":"password123","firstName":"A","lastName":"B"}' | jq
# Expected 400 with field-level error on "email"

# Register with duplicate email
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"alex@test.com","password":"password123","firstName":"A","lastName":"B"}' | jq
# Expected 409 "Email already in use"

# Login with wrong password
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alex@test.com","password":"wrongpassword"}' | jq
# Expected 401

# Change password with wrong current password
curl -s -X PATCH http://localhost:8080/api/users/me/password \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"currentPassword":"wrong","newPassword":"whatever"}' | jq
# Expected 400 "Current password is incorrect"

# Create book with negative price
curl -s -X POST http://localhost:8080/api/books \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Bad","author":"Bad","description":"x","price":-1.00,"stock":10,"categoryId":1}' | jq
# Expected 400 with field-level error on "price"
```

---

## Reference — Seeded Data

### Users (all password `12345678`)

| Email | Role |
|---|---|
| `pesho@bookstore.local` | USER |
| `anya@bookstore.local` | USER |
| `rumen@bookstore.local` | ADMIN |

### Books

23 books across 6 categories — Classic Literature, War & Historical Fiction, Crime & Mystery, Philosophy & Drama, Epic & Adventure, Science & Non-Fiction. All have Cloudinary CDN cover URLs seeded inline in `db/seed/V1002__seed_catalog.sql`.

### Migrations

Schema and seed data are split across two Flyway locations (schema always runs; seed is dev-profile only):

| File | Contents |
|---|---|
| `db/migration/V1__baseline_schema.sql` | Full DDL — all 7 tables, indexes, CHECK constraints, `version` columns (optimistic locking), and `token_valid_after` on users |
| `db/seed/V1001__seed_users.sql` | 3 seed users (dev only) |
| `db/seed/V1002__seed_catalog.sql` | 6 categories + 23 books with cover URLs (dev only) |
