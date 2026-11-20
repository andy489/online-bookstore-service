# Manual Test Guide

Base URL: `http://localhost:8080`

Replace `<TOKEN>` with the JWT returned from register or login.

---

## Auth

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123","firstName":"John","lastName":"Doe"}'
```
Expected: `201` with `{"token":"...","email":"test@test.com","role":"USER"}`

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123"}'
```
Expected: `200` with `{"token":"...","email":"test@test.com","role":"USER"}`

### Protected endpoint — no token (should fail)
```bash
curl http://localhost:8080/api/users/me
```
Expected: `403`

### Protected endpoint — with token
```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <TOKEN>"
```
Expected: `404` (endpoint not implemented yet — but auth passed)

---

## Validation errors

### Register with invalid email
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"not-an-email","password":"password123","firstName":"John","lastName":"Doe"}'
```
Expected: `400` with field-level error on `email`

### Register with short password
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test2@test.com","password":"short","firstName":"John","lastName":"Doe"}'
```
Expected: `400` with field-level error on `password`

### Register duplicate email
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"password123","firstName":"Jane","lastName":"Doe"}'
```
Expected: `409` with `"Email already in use"`

### Login with wrong password
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"wrongpassword"}'
```
Expected: `403`

---

## User profile

### Get own profile
```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <TOKEN>"
```
Expected: `200` with `{"id":1,"email":"test@test.com","firstName":"John","lastName":"Doe","role":"USER",...}`

### Update profile
```bash
curl -X PUT http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jane","lastName":"Smith"}'
```
Expected: `200` with updated profile

### Change password
```bash
curl -X PATCH http://localhost:8080/api/users/me/password \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"currentPassword":"password123","newPassword":"newpassword456"}'
```
Expected: `204`

### Change password — wrong current password
```bash
curl -X PATCH http://localhost:8080/api/users/me/password \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"currentPassword":"wrongpassword","newPassword":"newpassword456"}'
```
Expected: `400` with `"Current password is incorrect"`

---

## Categories

### List all (public)
```bash
curl http://localhost:8080/api/categories
```
Expected: `200` with array (empty initially)

### Create category (admin only — first register as admin or seed DB)
```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Fiction","description":"Fictional works"}'
```
Expected: `201` with `{"id":1,"name":"Fiction","description":"Fictional works"}`

### Create another category
```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Science","description":"Science books"}'
```

### Get category by id
```bash
curl http://localhost:8080/api/categories/1
```
Expected: `200`

### Update category
```bash
curl -X PUT http://localhost:8080/api/categories/1 \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Fiction","description":"Updated description"}'
```
Expected: `200`

### Delete category
```bash
curl -X DELETE http://localhost:8080/api/categories/2 \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```
Expected: `204`

### Create as USER (should fail)
```bash
curl -X POST http://localhost:8080/api/categories \
  -H "Authorization: Bearer <USER_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test"}'
```
Expected: `403`

---

## Books

### List/search books (public)
```bash
curl http://localhost:8080/api/books
```
Expected: `200` with `{"content":[],"page":0,"size":20,"totalElements":0,"totalPages":0}`

### Create book (admin only)
```bash
curl -X POST http://localhost:8080/api/books \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Dune","author":"Frank Herbert","description":"Epic sci-fi","price":14.99,"stock":50,"categoryId":1}'
```
Expected: `201`

### Create another book
```bash
curl -X POST http://localhost:8080/api/books \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Foundation","author":"Isaac Asimov","price":12.99,"stock":30,"categoryId":1}'
```

### Search by query
```bash
curl "http://localhost:8080/api/books?query=dune"
```
Expected: `200` with Dune in content

### Filter by price range
```bash
curl "http://localhost:8080/api/books?minPrice=10&maxPrice=13"
```
Expected: Foundation (12.99) only

### Paginate
```bash
curl "http://localhost:8080/api/books?page=0&size=1&sortBy=price&sortDir=asc"
```
Expected: 1 book, Foundation first

### Get book by id
```bash
curl http://localhost:8080/api/books/1
```
Expected: `200`

### Update book
```bash
curl -X PUT http://localhost:8080/api/books/1 \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"title":"Dune","author":"Frank Herbert","price":15.99,"stock":45,"categoryId":1}'
```
Expected: `200`

### Soft-delete book
```bash
curl -X DELETE http://localhost:8080/api/books/1 \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```
Expected: `204`

### Get deleted book (should 404)
```bash
curl http://localhost:8080/api/books/1
```
Expected: `404`

---

## Shopping Cart

> Assumes book with id `2` (Foundation, stock 30) still exists. Replace `<TOKEN>` with a USER token.

### Get cart (empty initially)
```bash
curl http://localhost:8080/api/cart \
  -H "Authorization: Bearer <TOKEN>"
```
Expected: `200` with `{"id":1,"items":[],"total":0}`

### Add item to cart
```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"bookId":2,"quantity":2}'
```
Expected: `200` with cart containing Foundation ×2, subtotal 25.98

### Add another book
```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"bookId":3,"quantity":1}'
```
Expected: `200` with 2 items in cart

### Add same book again (quantity increments)
```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"bookId":2,"quantity":1}'
```
Expected: `200` — Foundation quantity is now 3

### Update item quantity
```bash
curl -X PUT http://localhost:8080/api/cart/items/2 \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"quantity":5}'
```
Expected: `200` — Foundation quantity is now 5

### Remove item
```bash
curl -X DELETE http://localhost:8080/api/cart/items/3 \
  -H "Authorization: Bearer <TOKEN>"
```
Expected: `200` with only Foundation remaining

### Add non-existent book (should fail)
```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"bookId":9999,"quantity":1}'
```
Expected: `404`

### Clear cart
```bash
curl -X DELETE http://localhost:8080/api/cart \
  -H "Authorization: Bearer <TOKEN>"
```
Expected: `204`

---

## Orders

> Re-add items to cart before testing order placement.

### Re-add item after clear
```bash
curl -X POST http://localhost:8080/api/cart/items \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"bookId":2,"quantity":2}'
```

### Place order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <TOKEN>"
```
Expected: `201` with order containing Foundation ×2, totalPrice 25.98, status `PENDING`

### Cart should be empty after order
```bash
curl http://localhost:8080/api/cart \
  -H "Authorization: Bearer <TOKEN>"
```
Expected: `200` with empty items

### Get own order history
```bash
curl http://localhost:8080/api/orders/my \
  -H "Authorization: Bearer <TOKEN>"
```
Expected: `200` with array containing the placed order

### Get order by id (own order)
```bash
curl http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer <TOKEN>"
```
Expected: `200`

### Get order by id (another user's order — should 404)
```bash
curl http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer <OTHER_USER_TOKEN>"
```
Expected: `404`

### Place order with empty cart (should fail)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <TOKEN>"
```
Expected: `400` with `"Cannot place order with an empty cart"`

### Admin — list all orders
```bash
curl http://localhost:8080/api/orders \
  -H "Authorization: Bearer <ADMIN_TOKEN>"
```
Expected: `200` with all orders across all users

### Admin — update order status
```bash
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"status":"SHIPPED"}'
```
Expected: `200` with status `SHIPPED`

### Admin — update to CANCELLED
```bash
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"status":"CANCELLED"}'
```
Expected: `200` with status `CANCELLED`

### Admin — transition out of CANCELLED (should fail)
```bash
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Authorization: Bearer <ADMIN_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"status":"SHIPPED"}'
```
Expected: `400` with `"Cannot transition order out of CANCELLED status"`

### User — update status (should fail)
```bash
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"status":"SHIPPED"}'
```
Expected: `403`

---

## Admin setup

There is no register-as-admin endpoint (by design). To create an admin for testing, register normally
then update the role directly in the database:
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'admin@test.com';
```

Or register a user with the intended admin email and promote them:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"admin1234","firstName":"Admin","lastName":"User"}'
# then run the SQL above
```
