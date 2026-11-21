# Performance Bottlenecks — Analysis & Fixes

## Bottleneck 1 — N+1 Queries on Cart Load

### The Problem

A cart contains items, and each item references a book. With lazy loading (the JPA default), loading a cart triggers:

```
1 query  → load the Cart
N queries → load each CartItem (one per item)
N queries → load each Book (one per CartItem)
```

A cart with 5 items = **11 queries**. A cart with 20 items = **41 queries**. Every cart endpoint fires this: view cart, add item, update item, place order.

### Was It Actually a Problem Here?

**No — already fixed.** `CartRepositoryImpl.findByUserId` uses `JOIN FETCH`:

```java
"SELECT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.book WHERE c.user.id = :userId"
```

This tells JPA to load the cart, all its items, and all referenced books in a **single SQL query** with two JOINs. No lazy loading triggered at all.

Same pattern is applied in `OrderRepositoryImpl` for `findById`, `findByUserId`, and `findAll` — all use `JOIN FETCH` to avoid N+1 on order items and books.

### Result

Already 1 query regardless of cart size. No fix needed.

---

## Bottleneck 2 — N Separate UPDATEs in `placeOrder`

### The Problem

The original `placeOrder` called `bookRepository.save(book)` inside the loop — once per cart item:

```java
for (CartItem item : cart.getItems()) {
    book.setStock(available - fulfilled);
    bookRepository.save(book);  // ← one UPDATE to DB per book
    ...
}
```

With a 5-item cart this fired 5 separate `UPDATE books SET stock=... WHERE id=...` statements, each a round trip to PostgreSQL. The more items in the cart, the more round trips.

### Why It Was Unnecessary

JPA tracks every entity loaded within a `@Transactional` method — this is called the **persistence context** (or first-level cache). When you call `book.setStock(...)`, JPA marks that entity as "dirty" (changed). You do not need to call `save()` — JPA automatically flushes all dirty entities to the database when the transaction commits.

Calling `bookRepository.save(book)` inside the loop forced an **immediate** individual UPDATE per iteration instead of letting JPA batch them at the end.

### The Fix

Removed the `bookRepository.save(book)` call from the loop. JPA dirty checking handles the rest:

```java
// Before
book.setStock(available - fulfilled);
bookRepository.save(book);  // ← removed

// After
book.setStock(available - fulfilled);
// JPA marks book as dirty — will flush at transaction commit
```

`BookRepository` is no longer a dependency of `OrderService` at all — the import and constructor injection were removed.

### How JPA Flushes at Commit

```
Loop runs:
  book1.setStock(2)  → marked dirty
  book2.setStock(0)  → marked dirty
  book3.setStock(5)  → marked dirty

Transaction commits → JPA flush:
  UPDATE books SET stock=2, version=2 WHERE id=1 AND version=1
  UPDATE books SET stock=0, version=2 WHERE id=2 AND version=1
  UPDATE books SET stock=5, version=2 WHERE id=3 AND version=1
```

Same 3 UPDATEs, but sent together at the end rather than one-by-one during the loop. With JDBC batching enabled, PostgreSQL can process them in a single network round trip.

### Result

Removed redundant per-item `save()` calls. JPA dirty checking flushes all stock updates atomically at transaction commit. `BookRepository` dependency removed from `OrderService`.

---

## Bottleneck 3 — Missing Index on `books.deleted`

### The Problem

Every book search filters by `deleted = false`. Without an index on that column, PostgreSQL does a full table scan — reads every row to find the non-deleted ones.

```sql
-- Without index: full table scan
SELECT * FROM books WHERE deleted = false AND ...

-- With index: index scan, reads only matching rows
```

Fine at 23 books. Noticeable at 10,000+ books.

### Was It Actually a Problem Here?

**No — already in place.** `V1__init.sql` creates the index:

```sql
CREATE INDEX idx_books_deleted ON books (deleted);
```

### Result

Already indexed. No fix needed.

---

## Summary

| Bottleneck | Status | Action |
|---|---|---|
| N+1 on cart load | Already fixed via `JOIN FETCH` | None needed |
| N separate UPDATEs in `placeOrder` | Fixed | Removed `bookRepository.save()` from loop — JPA dirty checking flushes at commit |
| Missing index on `books.deleted` | Already in `V1__init.sql` | None needed |
