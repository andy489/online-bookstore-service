# Order Fulfillment — Original All-or-Nothing Approach

## How It Worked

The original `placeOrder` used a strict two-pass approach: validate everything first, then mutate. If any single item failed the stock check, the entire order was rejected before anything was touched.

---

## The Two-Pass Logic

### Pass 1 — Validate all items

Loop through every cart item and check if stock is sufficient:

```java
for (CartItem item : cart.getItems()) {
    if (item.getBook().getStock() < item.getQuantity()) {
        throw new InsufficientStockException(
            "Insufficient stock for '" + item.getBook().getTitle() + "': requested " +
            item.getQuantity() + ", available " + item.getBook().getStock());
    }
}
```

If any item failed → `InsufficientStockException` was thrown → `GlobalExceptionHandler` returned `409 Conflict`. The loop stopped at the first failure — it didn't report all problematic items, only the first one found.

### Pass 2 — Mutate (only reached if all items passed)

```java
for (CartItem item : cart.getItems()) {
    Book book = item.getBook();
    book.setStock(book.getStock() - item.getQuantity());
    bookRepository.save(book);

    order.getItems().add(new OrderItem(order, book, item.getQuantity(), book.getPrice()));
    total = total.add(book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
}
```

Stock was deducted by the exact requested quantity for every item. No adjustments, no skipping.

---

## Why Two Passes?

The separation existed to guarantee atomicity in the happy path: either all items are deducted or none are. Without it, a failure halfway through the mutation loop would leave some books deducted and others not — partial stock corruption.

The `@Transactional` annotation would have rolled back the whole transaction on exception, so the DB would never actually end up in a corrupted state. But the two-pass design made the intent explicit and readable: check first, act second.

---

## The Race Condition Protection — `@Version` (Optimistic Locking)

### What is optimistic locking?

There are two ways to prevent two transactions from corrupting the same row:

- **Pessimistic locking** — lock the row when you read it so nobody else can touch it until you're done (`SELECT FOR UPDATE`). Safe but slow — everyone else waits.
- **Optimistic locking** — don't lock anything on read. Instead, when you write, verify that nobody else changed the row since you read it. If they did, your write fails. This is what `@Version` does.

It's called "optimistic" because it assumes conflicts are rare — most of the time nobody else is touching the same row, so you don't pay the cost of a lock.

### How `@Version` works in the code

The `Book` entity has one extra field:

```java
@Version
private Long version;
```

That's it. JPA manages this field automatically — you never read or write it yourself. It maps to a `version` column in the `books` table in the database.

**On every read:** JPA loads the current version number alongside the other fields.

**On every update:** JPA generates SQL like this instead of a plain UPDATE:

```sql
-- Plain UPDATE (no version check — dangerous):
UPDATE books SET stock = 1 WHERE id = 1;

-- What JPA actually generates with @Version:
UPDATE books SET stock = 1, version = 2 WHERE id = 1 AND version = 1;
```

The `AND version = 1` is the guard. If the version in the DB is still 1 (nobody else changed it), the update affects 1 row and succeeds. JPA then increments version to 2.

If the version in the DB is already 2 (someone else updated it first), the update affects 0 rows. JPA detects this and throws `OptimisticLockException`, which causes `@Transactional` to roll back the entire transaction.

### The exact race scenario

Two users place orders for Book A (stock=3) at the same moment:

```
── Both users hit POST /api/orders simultaneously ──

User 1 transaction:                    User 2 transaction:
  reads Book A                           reads Book A
  → stock=3, version=1                   → stock=3, version=1

  Pass 1: 3 >= 2 ✓ (wants 2)            Pass 1: 3 >= 2 ✓ (wants 2)

  Pass 2: deduct stock                   Pass 2: deduct stock
  UPDATE books                           UPDATE books
    SET stock=1, version=2               SET stock=1, version=2
    WHERE id=1 AND version=1             WHERE id=1 AND version=1
  → 1 row affected ✓                   → 0 rows affected ✗
  → commits successfully                 → OptimisticLockException
                                         → @Transactional rolls back
                                         → no OrderItem saved
                                         → stock untouched
                                         → 409 Conflict returned
```

**Result:** User 1 gets their order. User 2 gets a 409. Book A stock is 1 (not -1). No corruption.

### Why not just use a database lock instead?

With `SELECT FOR UPDATE` (pessimistic), User 2's transaction would wait at the read step until User 1 finishes. Under high traffic — hundreds of users buying popular books — every transaction queues up. Response times spike.

With `@Version` (optimistic), both transactions run in parallel. Only the loser pays a cost (a retry or error), and that's rare. For an e-commerce store, most book purchases don't collide — optimistic locking is the right tradeoff.

### What happens to the loser?

The 409 response is returned to User 2's client. Their cart is still intact (the rollback undid everything including any cart changes). They can retry placing the order — by which point the remaining stock is 1, and if they wanted 2 they'll get the insufficient stock error from the validation pass instead.

---

## What the Error Response Looked Like

```json
HTTP 409 Conflict
{
  "message": "Insufficient stock for 'Dune': requested 3, available 1"
}
```

Only the first failing item was reported. If two items were out of stock, the user had to fix one, retry, and discover the second failure then.

---

## Why It Was Changed

| Problem | Impact |
|---|---|
| Whole order rejected if any item fails | User must manually find and remove items then retry |
| Only first failing item reported | Multiple failures require multiple retry attempts |
| No way to place a partial order | Lost sale if user wants the available items anyway |

The replacement (partial fulfillment) caps quantities to available stock, skips fully out-of-stock items, and reports all adjustments in a single response — removing the need for the user to retry at all.
