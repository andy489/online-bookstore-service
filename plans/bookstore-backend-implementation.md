# Bookstore Backend — Implementation Plan

> **Goal**: Demonstrate project structure and coding style for a production-grade Spring Boot REST API.
> **Constraints**: No Lombok · No `CrudRepository` · Custom repositories (EntityManager / JPQL) · JWT stateless auth
> **Status**: Backend complete. Frontend (Phase 10) is the only remaining work.

---

## Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.4.1, Gradle Kotlin DSL |
| Security | Spring Security 6, JWT (jjwt 0.12.6), DelegatingPasswordEncoder (BCrypt default) |
| Database | MySQL 8 (Docker), Flyway migrations (V1–V7, seed in `db/seed/`) |
| Caching | Redis 7 (Docker), Spring Cache with Jackson JSON serialization |
| Image hosting | Cloudinary CDN (`bookstore/covers/` folder) |
| API docs | springdoc-openapi 2.8.6 (Swagger UI at `/swagger-ui.html`) |
| Containers | Docker + docker-compose (db + redis + app) |

---

## Package Structure

```
com.bookstore
├── config/          # SecurityConfig, OpenApiConfig, CacheConfig, CloudinaryConfig, RateLimitConfig
├── security/        # JwtTokenProvider, JwtAuthFilter, UserDetailsServiceImpl
├── domain/          # JPA entities + enums (Role, OrderStatus)
├── repository/      # interfaces + EntityManager implementations
├── service/         # BookService, CartService, OrderService, OrderPlacer, OrderStatusUpdater, CloudinaryService, ...
├── controller/      # REST controllers
├── dto/
│   ├── request/     # RegisterRequest, LoginRequest, BookRequest, ...
│   └── response/    # UserResponse, BookResponse, PageResponse<T>, ErrorResponse, ...
└── exception/       # domain exceptions + GlobalExceptionHandler
```

**Rules enforced across all phases:**
1. Controllers never touch repositories — always go through a service
2. Services never return domain entities — always map to a DTO before returning
3. `@Transactional` lives only on service methods, never on repositories
4. Enums (`Role`, `OrderStatus`) live in `domain/`
5. All constructors are explicit — no generated builders

---

## Coding Style Conventions

1. JPA no-arg constructors are `protected` — not for general use
2. `@Transactional(readOnly = true)` on all read-only service methods
3. `@Valid` on all controller request DTOs with Bean Validation annotations
4. Explicit HTTP status codes: `201 CREATED` for POST, `204 NO_CONTENT` for DELETE
5. `BigDecimal` for all monetary values — never `double`/`float`
6. `Instant` for all timestamps

---

## ✅ Phase 0 — Project Bootstrap

**What:** Scaffolded the project foundation.

- Spring Boot 3.4.1, Java 21, Gradle Kotlin DSL
- `application.yml` — datasource + JWT config from env vars (`${DB_URL}`, `${JWT_SECRET}`, etc.)
- `.env` / `.env.example` — actual credentials gitignored, example committed
- Multi-stage `Dockerfile` — stage 1: Gradle build; stage 2: `eclipse-temurin:21-jre`
- `docker-compose.yml` — `db` (postgres:16-alpine) + `app` services

---

## ✅ Phase 1 — Domain Entities

**What:** All JPA entities and enums.

- `Role` enum — `ADMIN`, `USER`
- `OrderStatus` enum — `PENDING`, `SHIPPED`, `DELIVERED`, `CANCELLED`
- `User` — id, email (unique), passwordHash, firstName, lastName, role, timestamps
- `Category` — id, name (unique), description
- `Book` — id, title, author, description, price (`BigDecimal`), stock, coverImageUrl, category (`@ManyToOne LAZY`), deleted (soft-delete), `@Version` for optimistic locking, timestamps
- `Cart` — id, user (`@OneToOne`), items (`@OneToMany cascade=ALL orphanRemoval=true`), createdAt
- `CartItem` — id, cart, book, quantity
- `Order` — id, user, items (`@OneToMany cascade=ALL`), status, totalPrice, `@Version` for optimistic locking, timestamps
- `OrderItem` — id, order, book, quantity, **priceAtPurchase** (snapshot at order time — price changes don't affect past orders)
- `@PrePersist` / `@PreUpdate` lifecycle hooks for automatic timestamps

---

## ✅ Phase 2 — Security & Auth

**What:** Stateless JWT authentication with BCrypt password hashing.

- `UserDetailsServiceImpl` — loads by email (normalized to lowercase), maps `Role` → `ROLE_` prefixed authority
- `JwtTokenProvider` — `generateToken(UserDetails)`, `generateToken(UserDetails, Instant tokenValidAfter)`, `extractAllClaims(String)`, `isClaimsValid(Claims)`, `extractUsername(String)`, `extractJti(String)`, `extractExpiration(String)`, `isTokenValid(String)`; secret from `${JWT_SECRET}`; every token includes a `jti` UUID and a `role` claim; tokens generated at login also embed a `tva` (tokenValidAfter) epoch-seconds claim
- `JwtAuthenticationFilter` — calls `extractAllClaims(token)` once per request (single HMAC verify); reads `jti`, `username`, `role`, `tva` directly from the `Claims` object; validates `tokenIssuedAt >= tokenValidAfter` from the token itself with no DB lookup; checks JWT blocklist before accepting
- `SecurityConfig` — lambda-DSL `SecurityFilterChain`, stateless sessions, CSRF disabled, `@EnableMethodSecurity`
- Password encoder: `DelegatingPasswordEncoder` (BCrypt default) — forward-compatible, supports migrating algorithms without invalidating existing hashes
- `GlobalExceptionHandler` + `ErrorResponse` record — 400, 401, 403, 404, 409, 429, 500, 502 handlers; never leaks stack traces; `BadCredentialsException` → 401; `OptimisticLockingFailureException` → 409; Cloudinary failures → 502
- `POST /api/auth/register` — validate, hash, save, return JWT
- `POST /api/auth/login` — authenticate via `AuthenticationManager`, return JWT with `tva` claim embedded

---

## ✅ Phase 3 — User Profile

**What:** Authenticated user can view and update their own profile.

- `GET /api/users/me` — return `UserResponse`
- `PUT /api/users/me` — update firstName, lastName
- `PATCH /api/users/me/password` — verify current password, update hash, then **immediately blocklist the caller's current token** (calls `authService.logout(token)`) so the old token cannot be reused after a password change

---

## ✅ Phase 4 — Categories

**What:** Category CRUD — public reads, admin writes.

- `CategoryRepository` interface + `EntityManager` impl — `findAll`, `findById`, `findByName`, `save`, `delete`
- `CategoryService` — CRUD with duplicate-name check
- `GET /api/categories` and `GET /api/categories/{id}` — public
- `POST`, `PUT`, `DELETE /api/categories` — `@PreAuthorize("hasRole('ADMIN')")`

---

## ✅ Phase 5 — Books

**What:** Dynamic book search/filter/paginate, soft-delete, full CRUD for admins.

- `BookRepositoryImpl` — Criteria API for `search(query, categoryId, minPrice, maxPrice, page, size, sortBy, sortDir)` with optional predicates; parallel `countSearch` for `PageResponse`
- `PageResponse<T>` — hand-rolled generic pagination wrapper (content, page, size, totalElements, totalPages)
- `GET /api/books` — authenticated; supports `query`, `categoryId`, `minPrice`, `maxPrice`, `page`, `size`, `sortBy`, `sortDir`
- `GET /api/books/{id}` — authenticated
- `POST`, `PUT`, `DELETE /api/books` — ADMIN only; DELETE is soft (sets `deleted = true`)
- `POST /api/books/{id}/cover` — ADMIN only, `multipart/form-data`; uploads image to Cloudinary, stores returned CDN URL in DB

**Why Criteria API:** Dynamic optional filters can't be expressed safely with string-concatenated JPQL. Criteria API composes predicates type-safely.

---

## ✅ Phase 6 — Shopping Cart

**What:** Per-user cart with add/update/remove/clear operations.

- `CartRepositoryImpl` — `JOIN FETCH c.items i LEFT JOIN FETCH i.book` to avoid N+1 on CartItem → Book
- `CartService` — `getCart`, `addItem` (single-pass: finds existing item, enforces 100-distinct-item cap, checks out-of-stock, increments quantity if same book already in cart), `updateItem` (validates quantity ≤ stock), `removeItem`, `clearCart`
- `addItem` guards: `book.stock == 0` → 400 out of stock; `cart.size >= 100` for a new book → 400 cap exceeded; `existingQuantity + newQuantity > book.stock` → 400 insufficient stock
- `GET /api/cart`, `POST /api/cart/items`, `PUT /api/cart/items/{bookId}`, `DELETE /api/cart/items/{bookId}`, `DELETE /api/cart`

**Why deferred stock check for update:** Prevents reserving stock for items that may never be ordered. Add-item validates availability at insertion time; order placement validates again at fulfillment time as the final authority.

---

## ✅ Phase 7 — Orders

**What:** Partial-fulfillment order placement with stock deduction, order history, admin status management.

- `OrderService` — thin orchestrator; delegates to `OrderPlacer` and `OrderStatusUpdater`; has no `CartRepository` or `BookRepository` dependency; `placeOrder` retries up to 3 times on `ObjectOptimisticLockingFailureException`
- `OrderPlacer` — separate `@Component` bean with `public @Transactional place(User user)` method (extracted from `OrderService` to ensure Spring AOP proxy applies `@Transactional` — self-invocation on `private` methods bypasses the proxy):
  1. Load cart — throw `400` if empty
  2. **Single pass** over cart items — for each item:
     - `stock == 0` → skip item, record `OrderAdjustment` (fulfilled=0, reason="Out of stock")
     - `stock < requested` → cap `fulfilled = stock`, record `OrderAdjustment` with reason
     - `stock >= requested` → fulfill fully, no adjustment
  3. Deduct fulfilled quantity from each book's stock (optimistic lock via `@Version` on `Book` catches concurrent orders)
  4. If no items could be fulfilled → throw `400` "None of the items in your cart are in stock"
  5. Create `Order` (status `PENDING`) + `OrderItem`s with `priceAtPurchase` snapshot and fulfilled quantities
  6. Clear cart
  7. Return `OrderResponse` with `adjustments` list
- `OrderStatusUpdater` — separate `@Component` bean with `public @Transactional update(Long orderId, UpdateOrderStatusRequest)` method; enforces `ALLOWED_TRANSITIONS` state machine: `PENDING→{SHIPPED,CANCELLED}`, `SHIPPED→{DELIVERED,CANCELLED}`, `DELIVERED→{}`, `CANCELLED→{}`; restores book stock on `CANCELLED`
- `updateStatus` — ADMIN only; retries on `ObjectOptimisticLockingFailureException` (Order entity also has `@Version`)
- `GET /api/orders/my` — calling user's history (DESC by createdAt)
- `GET /api/orders/{id}` — own order (USER) or any order (ADMIN); 404 if USER requests another user's order
- `GET /api/orders` — ADMIN only, paginated
- `PATCH /api/orders/{id}/status` — ADMIN only

**Why `OrderPlacer` and `OrderStatusUpdater` are separate beans:** `@Transactional` on a method only works when called through the Spring proxy. Calling `this.doPlaceOrder()` from within the same class is a direct Java call — it bypasses the proxy entirely and the `@Transactional` annotation has no effect. Extracting to a separate `@Component` ensures the proxy is used.

**`OrderAdjustment` DTO fields:** `bookTitle`, `requested` (what user wanted), `fulfilled` (what was placed), `reason` (human-readable explanation).

---

## ✅ Phase 8 — Swagger / OpenAPI

**What:** Auto-generated interactive API documentation.

- `springdoc-openapi-starter-webmvc-ui:2.8.6`
- `OpenApiConfig` — `@OpenAPIDefinition` with title/version, `@SecurityScheme` for `bearerAuth` (Bearer JWT)
- All controllers annotated with `@Tag`, `@Operation`, `@SecurityRequirement`
- Swagger UI accessible at `http://localhost:8080/swagger-ui.html`

---

## ✅ Phase 9 — Flyway Database Migrations

**What:** Version-controlled schema and seed data — no `ddl-auto: create`.

| Migration | Location | Contents |
|---|---|---|
| `V1__init.sql` | `db/migration/` | Full DDL — all tables + base indexes |
| `V2__seed_users.sql` | `db/migration/` | 3 users in one INSERT: pesho (USER), rumen (ADMIN), anya (USER) |
| `V3__seed_catalog.sql` | `db/migration/` | 6 categories + 23 books; Cloudinary cover URLs inline in INSERT (never null) |
| `V4__add_indexes.sql` | `db/migration/` | Performance index: `idx_order_items_book_id` |
| `V5__add_constraints.sql` | `db/migration/` | CHECK constraints: non-negative stock, positive cart/order quantities |
| `V6__add_version_and_check_constraints.sql` | `db/migration/` | `version BIGINT DEFAULT 0` column on `orders` table (optimistic locking); CHECK constraints on role and status enum values |
| `V7__add_token_valid_after.sql` | `db/migration/` | `token_valid_after TIMESTAMPTZ` column on `users` table — used to invalidate all prior tokens on password change |

**Seed migration separation:** `V2__seed_users.sql` and `V3__seed_catalog.sql` are also duplicated under `db/seed/` as standalone seed-only copies. `application.yml` sets `flyway.locations: classpath:db/migration` only. `application-dev.yml` adds `classpath:db/seed` for the dev profile. Test configuration uses only `classpath:db/migration` — the seed data in `db/migration` runs in both environments.

**Seeded users (all password `1234`):**

| Email | Role |
|---|---|
| `pesho@gmail.com` | USER |
| `anya@bookstore.local` | USER |
| `rumen@bookstore.local` | ADMIN |

**Seeded books:** 23 books across 6 categories (Classic Literature, War & Historical Fiction, Crime & Mystery, Philosophy & Drama, Epic & Adventure, Science & Non-Fiction).

**Why V3 inlines cover URLs directly:** Earlier versions used a two-pass approach (V3 inserts books, V9 updates cover URLs). The consolidated V3 eliminates the intermediate state where books have no covers, simplifies the migration history, and makes the final seed state immediately clear.

**Why Flyway over Liquibase:** SQL-first, simpler mental model, no XML/YAML ceremony, strong Spring Boot autoconfiguration. Liquibase is better for teams needing rollback scripts — not needed here.

---

## ✅ Phase 11 — Redis Caching

**What:** In-memory cache for book catalog reads to reduce PostgreSQL load.

- `spring-boot-starter-cache` + `spring-boot-starter-data-redis`
- `CacheConfig` — `RedisCacheManager` with `GenericJackson2JsonRedisSerializer` backed by a custom `ObjectMapper` using `BasicPolymorphicTypeValidator` with an explicit allowlist (`com.bookstore.bookstore.*`, `java.util.*`, `java.math.*`, `java.time.*`) — prevents arbitrary class deserialization from Redis (replaces the insecure `LaissezFaireSubTypeValidator`); 5-minute TTL
- `@EnableCaching` on `BookstoreApplication`
- `BookService`:
  - `@Cacheable(value="books", key="'search:'+<all params>")` on `search(...)`
  - `@Cacheable(value="books", key="'id:'+#id")` on `getById(...)`
  - `@CacheEvict(value="books", allEntries=true)` on `create`, `update`, `delete`, `uploadCover`
- `application.yml` — `REDIS_HOST` / `REDIS_PORT` env vars, default `localhost:6379`
- `docker-compose.yml` — `redis:7-alpine`, `--appendonly yes` (AOF persistence), named `redis_data` volume, healthcheck; `app` depends on Redis healthy

**Why `allEntries=true` on eviction:** A price edit, stock change, or new book invalidates search result pages that may contain the changed book. Targeted key eviction would require caching the full search key set separately — not worth the complexity.

---

## ✅ Phase 12 — Cloudinary Image Hosting

**What:** Book cover images hosted on Cloudinary CDN instead of the Spring Boot server.

### How it works

1. Admin uploads a cover via `POST /api/books/{id}/cover` (multipart)
2. `CloudinaryService.uploadCover(file, publicId)` calls Cloudinary API, stores under `bookstore/covers/`
3. `BookService` stores that URL in `book.coverImageUrl`, evicts Redis cache
4. When a cover is **updated** (not first upload), `BookService` deletes the old Cloudinary asset (best-effort — failure is logged, not propagated) before uploading the new one, preventing orphaned CDN assets
5. `BookResponse.coverImageUrl` returns the CDN URL — frontend renders `<img src={book.coverImageUrl} />`

### Existing covers

All 23 seeded book covers were pre-uploaded to Cloudinary (`bookstore/covers/` folder in the `dmpzzernt` account). V9 migration wires these CDN URLs into the DB.

### Dev vs prod

| Environment | Cover URL format | How served |
|---|---|---|
| Dev (V8) | `/covers/dune.jpg` | Spring Boot static files |
| Prod (V9) | `https://res.cloudinary.com/dmpzzernt/...` | Cloudinary CDN globally |

V8 sets local paths; V9 immediately overwrites them with CDN URLs. Both run on every clean startup — final DB state is always the CDN URLs.

### Env vars required

```
CLOUDINARY_CLOUD_NAME=dmpzzernt
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
```

---

## ✅ Security — CVE Fixes

| CVE | Vulnerable component | Fix |
|---|---|---|
| CVE-2025-48924 | `commons-lang3 < 3.18.0` (StackOverflowError in ClassUtils) | Forced → `3.18.0` via `resolutionStrategy` |
| CVE-2025-48924 | `swagger-ui < 5.25.3` (transitive via springdoc) | Forced → `5.25.3` via `resolutionStrategy` |

---

## Entity Relationships

```
User  ──(1:1)──  Cart  ──(1:N)──  CartItem  ──(N:1)──  Book
User  ──(1:N)──  Order ──(1:N)──  OrderItem ──(N:1)──  Book
Book  ──(N:1)──  Category
```

---

## Risk Areas & Mitigations

| Risk | Mitigation |
|---|---|
| Stock race condition (concurrent orders) | `@Version` on `Book` and `Order` — optimistic locking; losing transaction gets `OptimisticLockException` → retried up to 3×, then 400 |
| `LazyInitializationException` on CartResponse | `JOIN FETCH` in cart query inside `@Transactional(readOnly=true)` |
| `TransactionRequiredException` in repositories | `@Transactional` only on service methods, never repositories |
| Spring Security 6 (`WebSecurityConfigurerAdapter` removed) | Lambda-DSL `SecurityFilterChain` from day one |
| Order total wrong after book re-pricing | `priceAtPurchase` snapshot in `OrderItem` |
| Credentials committed | Env vars only; `.env` gitignored; `.env.example` committed |
| Stale cache after book mutation | `@CacheEvict(allEntries=true)` on every write |

---

## Concurrency Model

**How requests run**

Tomcat keeps a thread pool (default: up to 200 threads). Every HTTP request gets its own thread from that pool. Two users placing an order at the same millisecond run `OrderService.placeOrder()` simultaneously on two different threads inside the same JVM — there is no serialization between them by default.

**The race condition**

Without any guard, two threads can both read the same stock value and both decide they can fulfill it:

```
Thread A reads book.stock = 3 → decides to fulfill 3 → writes stock = 0
Thread B reads book.stock = 3 → decides to fulfill 3 → writes stock = -3  ← oversell
```

**How `@Version` (optimistic locking) closes it**

Hibernate adds a `WHERE version = ?` clause to every `UPDATE`. Only one thread can win:

```sql
-- Thread A wins (version was 0):
UPDATE books SET stock = 0, version = 1 WHERE id = 42 AND version = 0;  -- 1 row affected ✓

-- Thread B loses (version is now 1, not 0):
UPDATE books SET stock = 0, version = 2 WHERE id = 42 AND version = 0;  -- 0 rows affected → OptimisticLockException
```

Thread B's transaction is rolled back automatically — no partial state, no oversell. The caller receives a 409.

**Why optimistic over pessimistic locking**

Pessimistic locking (`SELECT FOR UPDATE`) locks the row on the SELECT and holds it until commit. Every concurrent reader blocks every other reader. Under the typical read-heavy catalog workload, that's pure overhead on every request. Optimistic locking costs nothing on the read path and only fires on the rare actual collision — the right trade-off for e-commerce.

**`@Transactional` is what makes the rollback safe**

Both the read and the write happen inside a single `@Transactional` method. If `OptimisticLockException` fires mid-method, Spring rolls back the entire unit of work atomically: the order is not saved, the stock is not deducted, the cart is not cleared.

---

## 🔲 Phase 10 — Frontend (React + TypeScript)

**Not started.** This is the only remaining phase.

### Setup

- [ ] Scaffold with Vite: `npm create vite@latest bookstore-ui -- --template react-ts`
- [ ] Place under `frontend/` in repo root
- [ ] Add `axios`, `react-router-dom`, `zustand`
- [ ] Configure Vite proxy: `/api` → `http://localhost:8080` (avoids CORS in dev)

### Auth

- [ ] Register form (`POST /api/auth/register`)
- [ ] Login form (`POST /api/auth/login`)
- [ ] Store JWT in `localStorage`; attach as `Authorization: Bearer` via Axios interceptor
- [ ] Protected route wrapper — redirect to `/login` if no token

### Books & Browse

- [ ] Book list page — search bar, category filter, price range, sort controls
- [ ] Paginated results grid — book card with Cloudinary cover (`<img src={book.coverImageUrl} />`), title, author, price, "Add to Cart"
- [ ] Book detail page — full description, stock indicator, quantity selector

### Cart

- [ ] Cart page — list items, quantity controls, remove, total
- [ ] "Add to Cart" → `POST /api/cart/items`; cart icon shows item count in nav

### Checkout & Orders

- [ ] Checkout button → `POST /api/orders` → order confirmation page
- [ ] Order history page — `GET /api/orders/my` with status badge
- [ ] Order detail page — items, prices, status

### Admin Panel

- [ ] Admin-only route guard (`role === 'ADMIN'`)
- [ ] Category management — list, create, edit, delete
- [ ] Book management — list, create, edit, soft-delete; cover upload via `POST /api/books/{id}/cover`
- [ ] Order management — list all orders, status update dropdown

### Polish

- [ ] Loading states and error messages on all forms
- [ ] 404 and empty-state pages
- [ ] Responsive layout (mobile-friendly)

---

## ✅ Phase A — JWT Blocklist (Real Logout)

**Problem:** JWT is stateless — once issued, it's valid until expiry even if the user logs out. A stolen token stays usable.

**How it works:**
1. Every generated token includes a `jti` (JWT ID) claim — a UUID unique per token, and a `role` claim read directly by the filter
2. `POST /api/auth/logout` (authenticated) extracts the `jti` and stores it in Redis with TTL = remaining token lifetime
3. `JwtAuthenticationFilter` checks `TokenBlocklistService.isBlocked(jti)` on every request — blocked tokens are silently rejected

**Single-parse design:** `AuthService.logout` calls `jwtTokenProvider.extractAllClaims(token)` once and reads both `claims.getId()` (jti) and `claims.getExpiration()` from the returned `Claims` object — no double-parse.

**Why Redis:** TTL-based expiry is built in — no cleanup job needed. The key auto-deletes when the token would have expired anyway, so the blocklist never grows unboundedly.

**Files:**
- `JwtTokenProvider` — `generateToken` builder includes `.id(UUID.randomUUID())` (jti) and `.claim("role", role)`; `extractAllClaims(token)` returns `Claims` from a single HMAC verify; `isClaimsValid(Claims)` validates without re-parsing
- `TokenBlocklistService` — `block(jti, expiration)` → `Redis SET blocklist:{jti} 1 EX {ttl}`; `isBlocked(jti)` → `Redis EXISTS`
- `JwtAuthenticationFilter` — single `extractAllClaims` call; reads `jti`, username, role, `tva` from `Claims`; checks blocklist; validates `tva` claim
- `AuthService.logout(token)` — extracts claims once, delegates jti+expiration to `TokenBlocklistService`
- `AuthController POST /api/auth/logout` — authenticated endpoint, 204 No Content; expired tokens can still be blocklisted (removed pre-check)
- `SecurityConfig` — `/api/auth/logout` requires authentication (not `permitAll`)

---

## ✅ Phase B — Integration Tests (Testcontainers)

**Problem:** Unit tests mock the database and Redis. Bugs in Flyway migrations, Criteria API queries, or cache serialization go undetected until production.

**How it works:**
- `AbstractIntegrationTest` starts one `PostgreSQLContainer` and one `RedisContainer` as static fields — shared across all subclasses so Docker pays the startup cost once per test suite
- `@DynamicPropertySource` overrides `spring.datasource.*` and `spring.data.redis.*` with the container's random ports at runtime
- Flyway runs all V1–V7 migrations on the test DB before each test class — the same SQL that runs in production

**Tests written:**

| Class | What it tests |
|---|---|
| `BookRepositoryIntegrationTest` | `BookService.search()` with no filters, by query, by price range, by non-existent category, pagination |
| `TokenBlocklistIntegrationTest` | Token not blocked before logout, blocked after, different tokens have unique JTIs |
| `CacheIntegrationTest` | `@Cacheable` populates cache on first call, `@CacheEvict` on delete clears the entry |

**Files:**
- `src/test/java/.../integration/AbstractIntegrationTest.java` — shared base class
- `BookRepositoryIntegrationTest`, `TokenBlocklistIntegrationTest`, `CacheIntegrationTest`
- `src/test/resources/application.yml` — test config with dummy JWT secret and Cloudinary values

**Dependencies added:** `testcontainers:junit-jupiter`, `testcontainers:postgresql`, `testcontainers-redis:2.2.2`

---

## ✅ Phase C — Rate Limiting (Bucket4j + Redis)

**Problem:** `POST /api/auth/login`, `/register`, and `PATCH /api/users/me/password` are open to brute-force attacks — no limit on how many attempts an IP can make per minute.

**How it works:**
- Token bucket algorithm (Bucket4j): each IP gets a bucket of 5 tokens, refilled every minute
- Each request to a rate-limited path consumes 1 token; when empty → 429 Too Many Requests
- Bucket state lives in Redis (key `rate:{path}:{ip}`) — survives app restarts and works across multiple app instances
- `X-Forwarded-For` header is respected for IPs behind a load balancer; falls back to `request.getRemoteAddr()`

**Rate-limited paths:** `/api/auth/login`, `/api/auth/register`, `/api/users/me/password`

**Why token bucket:** Allows short bursts (all 5 attempts at once) while still bounding the sustained rate — better UX than a fixed window that resets abruptly.

**Files:**
- `RateLimitFilter extends OncePerRequestFilter` — checks path against `RATE_LIMITED_PATHS`, resolves IP, consults Redis bucket; returns 429 JSON with `Retry-After: 60` header on limit exceeded
- `RateLimitConfig` — Bucket4j `ProxyManager` bean backed by Redis
- `SecurityConfig` — `rateLimitFilter` registered before `jwtAuthenticationFilter`

**Dependencies added:** `bucket4j-core:8.14.0`, `bucket4j-redis:8.14.0`

---

## ✅ Phase D — Unit, Controller, and Integration Tests

**Problem:** Service logic, controller request validation, and cross-cutting flows (register→login→logout) had no automated test coverage.

**Unit tests (no Spring context, pure Mockito):**

| Class | What it tests |
|---|---|
| `OrderServiceUnitTest` | Full fulfillment, partial stock cap, zero-stock skip, all-out-of-stock exception, empty cart exception, exact stock deduction, cart cleared after order, price snapshot |
| `CartServiceUnitTest` | Add item, increment quantity on duplicate, item not found, update quantity, update non-existent item, remove item, empty cart, clear cart |

**Key unit test technique:** `Book.id` is a private field set only by JPA — unit tests inject it via reflection (`Book.class.getDeclaredField("id")`) so service code that compares IDs works without a real database.

**Controller tests (`@WebMvcTest` with MockMvc):**

| Class | What it tests |
|---|---|
| `AuthControllerMvcTest` | Register 201/400 (invalid email, short password, missing field)/409 (duplicate), login 200/400 (missing body)/403 (bad credentials) |

**`@WebMvcTest` configuration notes:**
- `@TestConfiguration` inner class provides a permissive `SecurityFilterChain` (all requests permitted, CSRF disabled) so controller logic is tested in isolation from security rules
- `@MockBean` for `JwtAuthenticationFilter`, `RateLimitFilter`, `JwtTokenProvider`, `UserDetailsService` satisfies `@Component` scan
- `@BeforeEach` configures filter mocks via `doAnswer` to call `chain.doFilter()` — prevents the mocked filters from silently swallowing requests

**Integration tests (Testcontainers — require Docker):**

| Class | What it tests |
|---|---|
| `OrderServiceIntegrationTest` | Place order with real DB: empty cart 400, full fulfillment, partial stock, all out of stock 400, stock deducted correctly, cart cleared |
| `AuthFlowIntegrationTest` | Register→login→logout full flow, duplicate email 409, blocklist effective after logout |

**Bug fixed during this phase:** `GlobalExceptionHandler` was missing a `@ExceptionHandler(BadCredentialsException.class)` — `AuthService.login()` throws it on wrong password, but it was falling through to the catch-all 500 handler. Added handler returning **401** (not 403 — wrong credentials are an authentication failure, not an authorization failure).

**Build configuration:**
- `testRuntimeOnly("org.junit.platform:junit-platform-launcher")` — required by Gradle 9.7
- `jvmArgs("-Dnet.bytebuddy.experimental=true")` on the test task — Mockito's Byte Buddy dependency doesn't officially support Java 25 without this flag

---

## ✅ Phase E — Stateless Token Invalidation (tva Claim + Password-Change Blocklist)

**Problem:** After a password change, the user's old token remained valid until expiry. Anyone who had stolen the token could continue using it.

**How it works — two complementary mechanisms:**

**1. `tva` (tokenValidAfter) claim:** Every token generated at login time embeds the user's `token_valid_after` epoch-seconds (from the DB) as a JWT claim. `JwtAuthenticationFilter` reads both the token's `issuedAt` date and the `tva` claim and rejects the token if `issuedAt < tva`. When a password change sets a new `token_valid_after` timestamp in the DB, all previously issued tokens (which embed the old, earlier `tva`) are automatically invalidated — no DB lookup per request needed.

**2. Immediate blocklist on password change:** `UserController.changePassword` takes the `Authorization` header, calls `userService.changePassword(...)`, then immediately calls `authService.logout(token)` to blocklist the caller's current token. This ensures the token used to perform the password change is invalidated right away, even before the `tva` guard would catch it.

**Why both mechanisms?** The `tva` claim handles all *other* tokens (e.g., multiple device sessions). The immediate blocklist handles the calling session's own token. Together they guarantee no token issued before the password change remains usable.

**Files changed:**
- `User` entity — `tokenValidAfter` field (`Instant`), set to `Instant.now()` on construction
- `V7__add_token_valid_after.sql` — `token_valid_after TIMESTAMPTZ NOT NULL DEFAULT NOW()` on `users`
- `JwtTokenProvider` — `generateToken(UserDetails, Instant tokenValidAfter)` overload embeds `tva` claim
- `AuthService.login` — fetches user to get `tokenValidAfter`, passes it to `generateToken`
- `JwtAuthenticationFilter` — reads `tva` claim; rejects if `issuedAt < tokenValidAfter`
- `UserController.changePassword` — added `@RequestHeader("Authorization")` param; calls `authService.logout(token)` after the password update
