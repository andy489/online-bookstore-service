# Concurrency & Stock Protection — Who Handles What

## The Problem

A Spring Boot app runs many threads simultaneously (Tomcat's default thread pool is 200). Two users can hit `POST /api/orders` at the exact same millisecond. Nothing in Java automatically prevents both threads from reading the same stock value and both deciding they can fulfill the order.

If left unprotected, this would cause **stock to go negative**:

```
Book A stock = 1

Thread 1 reads: stock = 1 → "enough, fulfill"
Thread 2 reads: stock = 1 → "enough, fulfill"

Thread 1 writes: stock = 1 - 1 = 0 ✓
Thread 2 writes: stock = 0 - 1 = -1 ✗ ← corruption
```

---

## Why Java-Level Synchronization Is Not the Answer

You could add `synchronized` to `placeOrder` in Java:

```java
public synchronized OrderResponse placeOrder(String email) { ... }
```

This would force threads to queue up — only one can execute at a time. It would work, but it has a critical flaw:

**It only protects within one JVM.**

If you run two instances of the app (horizontal scaling, load balancer, Docker replicas), each JVM has its own lock. Thread 1 on server A and Thread 2 on server B can still collide — they don't share the same Java lock.

The database is the only component shared by all app instances. So the database must be the enforcer.

---

## How PostgreSQL Handles It

PostgreSQL guarantees that writes to the same row are **serialized at the row level**. No matter how many threads or servers send an UPDATE for the same row simultaneously, the database processes them one at a time for that row.

```sql
-- Two threads send this simultaneously:
UPDATE books SET stock = 0, version = 2 WHERE id = 1 AND version = 1;
UPDATE books SET stock = 0, version = 2 WHERE id = 1 AND version = 1;

-- PostgreSQL serializes them internally:
-- First UPDATE  → 1 row affected → succeeds, version is now 2
-- Second UPDATE → 0 rows affected → WHERE version=1 no longer matches
```

This is **row-level locking** built into the database engine — not something you configure, it's how relational databases work.

---

## The Full Chain

```
App server 1 ──► Thread A ──┐
                             ├──► PostgreSQL row lock ──► one wins
App server 2 ──► Thread B ──┘                             one loses (0 rows affected)
```

**Java threads:** run freely in parallel — no synchronization in application code.  
**JPA `@Version`:** generates `WHERE version = X` on every UPDATE — the write-time guard.  
**PostgreSQL:** enforces that only one UPDATE to the same row wins, across all threads and all servers.

---

## Why `@Version` and Not a Plain UPDATE

Without `@Version`, both threads would run:

```sql
UPDATE books SET stock = 0 WHERE id = 1;
```

Both would succeed — PostgreSQL serializes them, but both see 1 row affected. Result: stock is 0 but two orders were created as if stock was 1. No error, silent corruption.

With `@Version`, the SQL becomes:

```sql
UPDATE books SET stock = 0, version = 2 WHERE id = 1 AND version = 1;
```

The second thread's version guard fails — 0 rows affected. JPA throws `OptimisticLockException`. `@Transactional` rolls back the entire transaction. The losing thread's order is never saved.

---

## The Three Layers Working Together

| Layer | Responsibility |
|---|---|
| `@Transactional` | Wraps the entire order operation — if anything fails, all DB changes roll back atomically |
| `@Version` | Generates the write-time version guard — detects when another transaction won the race; applied on both `Book` (stock deduction in `placeOrder`) and `Order` (status update in `updateStatus`) |
| PostgreSQL row locking | Serializes concurrent writes to the same row — the actual physical enforcement |

None of them alone is sufficient:
- `@Transactional` without `@Version` → no race detection, silent corruption possible
- `@Version` without `@Transactional` → exception thrown but partial changes already committed
- PostgreSQL without `@Version` → concurrent UPDATEs both succeed, no way to detect the collision

## Automatic Retry on Collision

Rather than returning a 409 immediately, `OrderService.placeOrder` retries up to 3 times on `ObjectOptimisticLockingFailureException`. On each retry, the transaction re-reads current stock — if the race winner left stock, the loser's retry may succeed (possibly with a partial fulfillment adjustment). Only after 3 consecutive failures does the user receive a `400` error.

The same retry logic applies in `OrderStatusUpdater.update` for order status transitions, since `Order` also carries a `@Version` column.

---

## Scaling Implication

This design works correctly whether you run:
- 1 app instance with 1 thread
- 1 app instance with 200 threads
- 10 app instances behind a load balancer with 2000 total threads

All of them share the same PostgreSQL — the row-level lock is the single enforcement point regardless of how many JVMs are running.
