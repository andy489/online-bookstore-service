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
| Security | Spring Security 6, JWT (jjwt 0.12.6), PBKDF2 password encoder |
| Database | PostgreSQL 16 (Docker), Flyway migrations (V1–V9) |
| Caching | Redis 7 (Docker), Spring Cache with Jackson JSON serialization |
| Image hosting | Cloudinary CDN (`bookstore/covers/` folder) |
| API docs | springdoc-openapi 2.8.6 (Swagger UI at `/swagger-ui.html`) |
| Containers | Docker + docker-compose (db + redis + app) |

---

## Package Structure

```
com.bookstore
├── config/          # SecurityConfig, OpenApiConfig, CacheConfig, CloudinaryConfig
├── security/        # JwtTokenProvider, JwtAuthFilter, UserDetailsServiceImpl
├── domain/          # JPA entities + enums (Role, OrderStatus)
├── repository/      # interfaces + EntityManager implementations
├── service/         # BookService, CartService, OrderService, CloudinaryService, ...
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
- `Order` — id, user, items (`@OneToMany cascade=ALL`), status, totalPrice, timestamps
- `OrderItem` — id, order, book, quantity, **priceAtPurchase** (snapshot at order time — price changes don't affect past orders)
- `@PrePersist` / `@PreUpdate` lifecycle hooks for automatic timestamps

---

## ✅ Phase 2 — Security & Auth

**What:** Stateless JWT authentication with PBKDF2 password hashing.

- `UserDetailsServiceImpl` — loads by email, maps `Role` → `ROLE_` prefixed authority
- `JwtTokenProvider` — `generateToken`, `extractUsername`, `isTokenValid`; secret from `${JWT_SECRET}`
- `JwtAuthenticationFilter` — parses Bearer token, sets `SecurityContextHolder`; on invalid token clears context and continues (never throws)
- `SecurityConfig` — lambda-DSL `SecurityFilterChain`, stateless sessions, CSRF disabled, `@EnableMethodSecurity`
- Password encoder: `Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()` (SHA-256, 310 000 iterations)
- `GlobalExceptionHandler` + `ErrorResponse` record — 404, 409, 400, 403, 500 handlers; never leaks stack traces
- `POST /api/auth/register` — validate, hash, save, return JWT
- `POST /api/auth/login` — authenticate, return JWT

**Why PBKDF2 over BCrypt:** More configurable iteration count, same security model, Spring Security ships defaults for it.

---

## ✅ Phase 3 — User Profile

**What:** Authenticated user can view and update their own profile.

- `GET /api/users/me` — return `UserResponse`
- `PUT /api/users/me` — update firstName, lastName
- `PATCH /api/users/me/password` — verify current password, then update hash

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
- `CartService` — `getCart`, `addItem` (increments quantity if same book already in cart), `updateItem`, `removeItem`, `clearCart`
- Stock is **not** checked here — only validated when the user actually places an order
- `GET /api/cart`, `POST /api/cart/items`, `PUT /api/cart/items/{bookId}`, `DELETE /api/cart/items/{bookId}`, `DELETE /api/cart`

**Why deferred stock check:** Prevents reserving stock for items that may never be ordered, avoids phantom holds when users browse and abandon carts.

---

## ✅ Phase 7 — Orders

**What:** Atomic order placement with stock deduction, order history, admin status management.

- `OrderService.placeOrder` — single `@Transactional` method:
  1. Load cart — throw `InvalidOperationException` if empty
  2. **Validation pass** — check `book.stock >= item.quantity` for ALL items; throw `InsufficientStockException` on first failure (409)
  3. **Mutation pass** — deduct stock from each book (optimistic lock via `@Version` catches concurrent orders)
  4. Create `Order` (status `PENDING`) + `OrderItem`s with `priceAtPurchase` snapshot
  5. `clearCart`
- `updateStatus` — ADMIN only; blocks any transition out of `CANCELLED`
- `GET /api/orders/my` — calling user's history (DESC by createdAt)
- `GET /api/orders/{id}` — own order (USER) or any order (ADMIN); 403 if USER requests another user's order
- `GET /api/orders` — ADMIN only
- `PATCH /api/orders/{id}/status` — ADMIN only

**Why two-pass validation/mutation:** Ensures we never partially deduct stock — either all items pass or none are touched.

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

| Migration | Contents |
|---|---|
| `V1__init.sql` | Full DDL — all tables + indexes |
| `V2__seed_admin.sql` | `pesho@gmail.com` / password `1234` / role `USER` |
| `V3__seed_books.sql` | 5 categories + 18 famous books |
| `V4__seed_lehane_books.sql` | Dennis Lehane — Mystic River, Gone Baby Gone, Shutter Island |
| `V5__seed_rumen_admin.sql` | `rumen@bookstore.local` / password `1234` / role `ADMIN` |
| `V6__seed_anya_user.sql` | `anya@bookstore.local` / password `1234` / role `USER` |
| `V7__seed_gattaca_related_books.sql` | Brave New World (Huxley) + The Selfish Gene (Dawkins) + Science & Non-Fiction category |
| `V8__set_cover_image_urls.sql` | Sets local `/covers/{slug}.jpg` paths for all 23 books |
| `V9__set_cloudinary_cover_urls.sql` | Overwrites with Cloudinary CDN URLs — final prod-ready state |

**Seeded users (all password `1234`):**

| Email | Role |
|---|---|
| `pesho@gmail.com` | USER |
| `anya@bookstore.local` | USER |
| `rumen@bookstore.local` | ADMIN |

**Seeded books:** 23 books across 6 categories (Classic Literature, War & Historical Fiction, Crime & Mystery, Philosophy & Drama, Epic & Adventure, Science & Non-Fiction).

**Why Flyway over Liquibase:** SQL-first, simpler mental model, no XML/YAML ceremony, strong Spring Boot autoconfiguration. Liquibase is better for teams needing rollback scripts — not needed here.

---

## ✅ Phase 11 — Redis Caching

**What:** In-memory cache for book catalog reads to reduce PostgreSQL load.

- `spring-boot-starter-cache` + `spring-boot-starter-data-redis`
- `CacheConfig` — `RedisCacheManager` with `GenericJackson2JsonRedisSerializer` (JSON, no `Serializable` required on DTOs), 5-minute TTL
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
3. Cloudinary returns a permanent `https://res.cloudinary.com/...` URL
4. `BookService` stores that URL in `book.coverImageUrl`, evicts Redis cache
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
| Stock race condition (concurrent orders) | `@Version` on `Book` — optimistic locking; second concurrent order gets `OptimisticLockException` → 409 |
| `LazyInitializationException` on CartResponse | `JOIN FETCH` in cart query inside `@Transactional(readOnly=true)` |
| `TransactionRequiredException` in repositories | `@Transactional` only on service methods, never repositories |
| Spring Security 6 (`WebSecurityConfigurerAdapter` removed) | Lambda-DSL `SecurityFilterChain` from day one |
| Order total wrong after book re-pricing | `priceAtPurchase` snapshot in `OrderItem` |
| Credentials committed | Env vars only; `.env` gitignored; `.env.example` committed |
| Stale cache after book mutation | `@CacheEvict(allEntries=true)` on every write |

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

## 🔲 Optional Future Work

- [ ] JWT blocklist via Redis (real logout — store invalidated JTI with TTL matching token expiry)
- [ ] Testcontainers integration tests (PostgreSQL + Redis)
- [ ] Rate limiting on auth endpoints
- [ ] Move plan to `plans/completed/` when Phase 10 is done
