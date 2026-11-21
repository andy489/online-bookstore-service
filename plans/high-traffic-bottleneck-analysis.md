# High-Traffic Bottleneck Analysis & Fixes

Scenario: many concurrent users browsing books, adding to cart, placing orders simultaneously.

---

## Fix 1 — HikariCP Connection Pool Configuration

### The Problem

Spring Boot auto-configures HikariCP (the JDBC connection pool) with defaults when no explicit config is provided. The default `maximum-pool-size` is **10 connections**.

Tomcat's default thread pool is **200 threads**. Each incoming HTTP request gets one thread. When a thread needs to run a database query, it asks HikariCP for a connection. With only 10 available:

```
200 incoming requests hit simultaneously
→ 10 get a DB connection and run
→ 190 threads sit blocked, waiting for a connection to free up
→ HikariCP default connection-timeout is 30 seconds
→ after 30s waiting, the thread throws an exception → 500 error for the user
```

This is the most common cause of Spring Boot apps falling over under load. The app doesn't crash — it just makes everyone wait 30 seconds and then returns errors. It looks like a slow DB but it's actually a starved pool.

### The Fix

Added explicit HikariCP configuration to `application.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20       # up from default 10
      minimum-idle: 5             # keep 5 warm connections ready
      connection-timeout: 10000   # fail fast in 10s instead of waiting 30s
      idle-timeout: 300000        # release idle connections after 5 min
      max-lifetime: 600000        # recycle connections every 10 min (prevents stale)
      pool-name: BookstorePool    # named pool — visible in logs and metrics
```

**Why 20 and not 200?** Each PostgreSQL connection is a backend OS process consuming ~5MB RAM. 200 connections = 1GB RAM just for connection overhead before any query runs. PostgreSQL handles ~100–200 concurrent connections well. Setting the app pool to 20 leaves headroom for other clients (DBeaver, admin tools, monitoring agents) while giving the app enough parallelism for typical traffic. The right number scales with your PostgreSQL server size.

**Why `connection-timeout: 10000`?** Failing fast in 10 seconds instead of 30 means users get an error sooner (better than a 30-second hang) and the thread is released sooner (less memory pressure under overload).

**Why `max-lifetime: 600000`?** Long-lived DB connections can go stale — the network or the DB server may close them without the app knowing. Recycling connections every 10 minutes prevents "connection reset" errors that appear randomly in production.

---

## Fix 2 — Pagination on `GET /api/orders` (Admin)

### The Problem

The original `getAllOrders` in `OrderRepositoryImpl` had no pagination:

```java
"SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.book ORDER BY o.createdAt DESC"
```

This loads **every single order**, with **every order item**, with **every book**, into JVM heap memory in one query. At scale:

- 10,000 orders × 3 items average = 30,000 `OrderItem` objects
- 30,000 `Book` objects (duplicated per item)
- All mapped to DTOs and serialized to JSON in one response

This causes two problems:
1. **Memory pressure** — a single admin request can spike heap usage by hundreds of MB
2. **Response time** — the query, mapping, and JSON serialization of 30,000 objects takes seconds

This is an admin-only endpoint so regular users never hit it, but one admin clicking "view all orders" can cause a GC spike that affects all users on the same JVM.

### The Fix

Added `page` and `size` parameters to the repository, service, and controller.

**`OrderRepositoryImpl`** — paginated query + count query:
```java
public List<Order> findAll(int page, int size) {
    return em.createQuery("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.book ORDER BY o.createdAt DESC", Order.class)
            .setFirstResult(page * size)   // SQL OFFSET
            .setMaxResults(size)            // SQL LIMIT
            .getResultList();
}

public long countAll() {
    return em.createQuery("SELECT COUNT(o) FROM Order o", Long.class).getSingleResult();
}
```

**`OrderController`** — `page` and `size` query params with defaults:
```java
@GetMapping
public ResponseEntity<PageResponse<OrderResponse>> getAllOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(orderService.getAllOrders(page, size));
}
```

Returns the same `PageResponse<T>` format already used for books — consistent API shape:
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 10000,
  "totalPages": 500
}
```

Each request now loads at most 20 orders regardless of total order count.

---

## Fix 3 — Hot Book Row Contention in `placeOrder`

### The Problem

When many users place orders for the same popular book simultaneously, all their transactions try to UPDATE the same `books` row. PostgreSQL serializes writes to the same row — only one transaction can hold the row lock at a time. The others wait.

This is **correct behavior** (prevents overselling) and cannot be eliminated — it is the fundamental tradeoff of ensuring stock accuracy under concurrency. The only mitigation is to keep the transaction as short as possible so the row lock is held for the minimum time.

### The Fix

No code change needed — the current implementation already does this correctly:
- The transaction does no external calls (no Cloudinary, no Redis, no HTTP)
- The transaction does no unnecessary queries
- The `bookRepository.save()` per-item call was already removed (see performance-bottlenecks.md)

The transaction is as lean as it can be. Documented here for completeness.

---

## Fix 4 — `CacheEvict(allEntries = true)` Under Write Contention

### The Problem

Every book write operation evicts the entire `books` Redis cache:

```java
@CacheEvict(value = "books", allEntries = true)
```

Under a scenario where many admins perform bulk writes (price updates, new book imports), the cache is constantly invalidated. Every eviction means the next read is a cache miss — goes to PostgreSQL. With rapid enough evictions the cache provides no benefit and PostgreSQL absorbs all read traffic.

### Why It Was Not Changed

This is an acceptable tradeoff for this project:
- Only admins can write books — regular users cannot trigger evictions
- Book writes are rare compared to reads
- Targeted key eviction (evicting only the changed book's cache entry) would not help for `search` results, since a price change affects multiple search result pages that may contain the book
- A full evict-on-write is the correct simple solution here

Documented as a known limitation. At very high admin write volume (bulk import pipelines), this could be replaced with a write-through cache strategy — but that is out of scope.

---

## Fix 5 — Double Query Per Book Search (countSearch)

### The Problem

Every `GET /api/books` call fires two queries:

```java
List<Book> content = bookRepository.search(...);    // fetch the page
long total = bookRepository.countSearch(...);        // count total matches
```

The count query is necessary to compute `totalPages`. Both queries run the same WHERE predicates against the `books` table. At high traffic with no cache, this means every book search is 2 DB round trips.

### The Fix

No code change needed — **Redis cache already mitigates this**. The `@Cacheable` on `BookService.search()` caches the full `PageResponse` (including `totalPages`) keyed by all search parameters. The double query only fires on a cache miss. Subsequent identical requests are served from Redis in microseconds.

Documented as a known characteristic, not a bug.

---

## Fix 6 — Missing Index on `order_items.book_id`

### The Problem

`V1__init.sql` created an index on `order_items.order_id` (used when loading an order's items) but not on `order_items.book_id`:

```sql
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
-- book_id had no index
```

The existing queries always look up order items by `order_id`, so this was not a problem for current code. But any future query asking "which orders contain book X?" — inventory reports, analytics, recall notices — would cause a full sequential scan of the entire `order_items` table:

```sql
-- Without index: full scan of all order_items rows
SELECT * FROM order_items WHERE book_id = 5;
```

At millions of order items this is seconds of query time. Adding the index now costs nothing and prevents the problem before it occurs.

### The Fix

Added `V10__add_order_items_book_id_index.sql`:

```sql
CREATE INDEX idx_order_items_book_id ON order_items (book_id);
```

Flyway applies this on next startup. For a live database with existing data, PostgreSQL builds the index without locking the table (it uses a concurrent build strategy for new indexes).

---

## Fix 7 — JDBC Batching

### The Problem

Without batching, every `INSERT` or `UPDATE` is a separate network round trip from the app to PostgreSQL. When placing an order with 5 items, the transaction sends:

```
→ INSERT INTO orders ...          (1 round trip)
→ INSERT INTO order_items ...     (1 round trip)
→ INSERT INTO order_items ...     (1 round trip)
→ INSERT INTO order_items ...     (1 round trip)
→ INSERT INTO order_items ...     (1 round trip)
→ INSERT INTO order_items ...     (1 round trip)
→ UPDATE carts ...                (1 round trip)
= 7 round trips
```

Each round trip has network latency (even on localhost, this is ~0.1–0.5ms per round trip). At 100 concurrent orders, this adds up.

With JDBC batching enabled, Hibernate groups statements of the same type and sends them together:

```
→ INSERT INTO orders ...
→ INSERT INTO order_items (5 rows in one batch)   (1 round trip)
→ UPDATE carts ...
= 3 round trips
```

### The Fix

Added to `application.yml` under `spring.jpa.properties.hibernate`:

```yaml
jdbc:
  batch_size: 25
order_inserts: true
order_updates: true
```

- `batch_size: 25` — group up to 25 statements into one batch
- `order_inserts: true` — sort INSERTs by entity type so same-table inserts can be batched together
- `order_updates: true` — same for UPDATEs

This requires no code changes — Hibernate handles it transparently.

---

## Summary of Changes

| Fix | Files Changed | Type |
|---|---|---|
| 1. HikariCP pool config | `application.yml` | Config |
| 2. Paginate `GET /api/orders` | `OrderRepository`, `OrderRepositoryImpl`, `OrderService`, `OrderController` | Code |
| 3. Hot book contention | None — already optimal | — |
| 4. Cache eviction | None — acceptable tradeoff | — |
| 5. Double query per search | None — mitigated by Redis | — |
| 6. Missing index on `order_items.book_id` | `V10__add_order_items_book_id_index.sql` | Migration |
| 7. JDBC batching | `application.yml` | Config |
