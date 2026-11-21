# Partial Order Fulfillment

## The Problem It Solves

Before this change, if a user had 2 books in their cart and one was out of stock, the entire order was rejected with a 400 error. The user had to manually remove the out-of-stock item and try again.

With partial fulfillment, the order goes through for whatever is available. Items with no stock are skipped, items with less stock than requested are capped, and the response tells the user exactly what changed.

---

## How It Works — Step by Step

Consider a cart with:
- Book A: requested 3, stock = 3 → fully available
- Book B: requested 5, stock = 2 → partially available
- Book C: requested 1, stock = 0 → out of stock

`POST /api/orders` does the following for each cart item:

```
1. stock == 0?
     → skip the item entirely
     → record adjustment: fulfilled=0, reason="Out of stock"

2. stock < requested?
     → cap: fulfilled = stock (e.g. 2 instead of 5)
     → record adjustment: fulfilled=2, reason="Only 2 in stock"

3. stock >= requested?
     → fulfill fully, no adjustment recorded
```

After processing all items:
- If **nothing** could be fulfilled (all items out of stock) → `400 Bad Request` — no order created
- Otherwise → order is saved with the fulfilled quantities, cart is cleared, response includes the `adjustments` list

---

## What Changed in the Code

### New file: `OrderAdjustment.java`
```
src/main/java/com/bookstore/bookstore/dto/response/OrderAdjustment.java
```
A DTO with four fields:

| Field       | Type   | Example             |
|-------------|--------|---------------------|
| `bookTitle` | String | `"Dune"`            |
| `requested` | int    | `5`                 |
| `fulfilled` | int    | `2`                 |
| `reason`    | String | `"Only 2 in stock"` |

### Modified: `OrderResponse.java`
Added `List<OrderAdjustment> adjustments` field. Added a second static factory `from(Order, List<OrderAdjustment>)` used only by `placeOrder`. All other calls (get order, update status) use the original `from(Order)` which sets `adjustments` to an empty list.

### Modified: `OrderService.placeOrder()`
Replaced the two-pass validate-then-mutate loop with a single pass that:
1. Caps `fulfilled = Math.min(requested, available)`
2. Records an `OrderAdjustment` if `fulfilled < requested` or `fulfilled == 0`
3. Only adds an `OrderItem` if `fulfilled > 0`
4. Throws `400` only if the entire order is empty after processing

---

## Example Response

Order placed with Book A (fully fulfilled), Book B (capped), Book C (skipped):

```json
{
  "id": 7,
  "status": "PENDING",
  "totalPrice": 44.95,
  "items": [
    {
      "bookId": 1,
      "title": "Book A",
      "quantity": 3,
      "priceAtPurchase": 12.99,
      "subtotal": 38.97
    },
    {
      "bookId": 2,
      "title": "Book B",
      "quantity": 2,
      "priceAtPurchase": 2.99,
      "subtotal": 5.98
    }
  ],
  "adjustments": [
    {
      "bookTitle": "Book B",
      "requested": 5,
      "fulfilled": 2,
      "reason": "Only 2 in stock"
    },
    {
      "bookTitle": "Book C",
      "requested": 1,
      "fulfilled": 0,
      "reason": "Out of stock"
    }
  ],
  "createdAt": "2026-11-21T20:00:00Z",
  "updatedAt": "2026-11-21T20:00:00Z"
}
```

If `adjustments` is an **empty array** `[]`, everything was fulfilled exactly as requested.

---

## Edge Cases

| Situation                      | Result                                                     |
|--------------------------------|------------------------------------------------------------|
| All items in stock             | `adjustments: []`, order placed normally                   |
| Some items partially available | Order placed, `adjustments` lists what was capped          |
| Some items fully out of stock  | Order placed without those items, `adjustments` lists them |
| All items out of stock         | `400` — "None of the items in your cart are in stock"      |
| Empty cart                     | `400` — "Cannot place order with an empty cart"            |

---

## Race Condition Protection — `@Version` (Optimistic Locking)

### What partial fulfillment solves vs what it does not

Partial fulfillment handles **known stock shortages at read time** — when the transaction starts and loads the cart, it can already see `stock < requested` and adjusts accordingly. This is a business logic decision made within a single transaction.

It does **not** eliminate the race condition window. That window exists between the moment you read the stock value and the moment you write the new value back. Two transactions can both read the same stock, both decide to fulfill, and then collide on the write. `@Version` is what handles that case.

### How `@Version` works

The `Book` entity has one field:

```java
@Version
private Long version;
```

JPA manages this automatically — you never read or write it. It maps to a `version` column in the `books` table.

**On every read:** JPA loads the current version alongside other fields.

**On every update:** JPA adds a version guard to the SQL:

```sql
-- What JPA actually generates:
UPDATE books SET stock = 1, version = 2 WHERE id = 1 AND version = 1;
```

If the version in the DB is still 1 → 1 row affected → success, version incremented to 2.
If another transaction already updated it → version is now 2 → 0 rows affected → `OptimisticLockException` → `@Transactional` rolls back everything.

### The race scenario in v2

Two users place orders simultaneously. Book A has stock=3. User 1 wants 2, User 2 wants 2. Partial fulfillment would cap both to 2 since 3 >= 2 for both — but they can't both get 2.

```
User 1 transaction:                    User 2 transaction:
  reads Book A                           reads Book A
  → stock=3, version=1                   → stock=3, version=1

  stock(3) >= requested(2)               stock(3) >= requested(2)
  → fulfill 2, no adjustment             → fulfill 2, no adjustment

  UPDATE books                           UPDATE books
    SET stock=1, version=2                 SET stock=1, version=2
    WHERE id=1 AND version=1               WHERE id=1 AND version=1
  → 1 row affected ✓                   → 0 rows affected ✗
  → commits                              → OptimisticLockException
  → order saved, cart cleared            → @Transactional rolls back
                                         → no order saved
                                         → cart still intact
                                         → 409 returned
```

User 2 gets a 409. Their cart is intact. On retry, the transaction reads Book A fresh — stock is now 1, they requested 2. Partial fulfillment now kicks in: `fulfilled = min(2, 1) = 1`, adjustment recorded, order placed with quantity 1.

### Key distinction

| Scenario | Handled by |
|---|---|
| Stock already low when order starts | Partial fulfillment (caps/skips at read time) |
| Two transactions collide on the same row | `@Version` optimistic locking (write-time guard) |

They solve different problems and work together — partial fulfillment reduces the chance of a 409 on retry (since after the race, the loser can still place a partial order), while `@Version` guarantees stock never goes negative regardless of concurrency.

---

Rejecting the whole order forces the user to find and remove the out-of-stock item manually, then retry. That friction loses sales. Fulfilling what's available and reporting adjustments is standard e-commerce behavior — the user gets what they can and is clearly informed about what changed. The `adjustments` field gives the frontend everything it needs to show a confirmation screen.
