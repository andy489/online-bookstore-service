# Bookstore Backend — Implementation Plan

> **Goal**: Demonstrate project structure and coding style. Full endpoint coverage is not required.
> **Constraints**: No Lombok · No `CrudRepository` · Custom repositories (EntityManager / JPQL) · JWT stateless auth

---

## Package Structure

```
com.bookstore
├── config/                    # SecurityConfig, JwtConfig, OpenApiConfig
├── security/                  # JwtTokenProvider, JwtAuthFilter, UserDetailsServiceImpl
├── domain/                    # JPA entities + enums (Role, OrderStatus)
├── repository/                # interfaces + EntityManager implementations
├── service/                   # one service per domain area
├── controller/                # REST controllers
├── dto/
│   ├── request/               # RegisterRequest, LoginRequest, BookRequest, etc.
│   └── response/              # UserResponse, BookResponse, PageResponse<T>, etc.
├── exception/                 # domain exceptions + GlobalExceptionHandler
└── BookstoreApplication.java
```

**Rules enforced across all phases:**
1. Controllers never touch repositories — always go through a service
2. Services never return domain entities — always map to a DTO before returning
3. `@Transactional` lives only on service methods, never on repositories
4. Enums (`Role`, `OrderStatus`) live in `domain/`
5. All constructors are explicit — no generated builders

---

## Repository Strategy

No `CrudRepository`. Two patterns used selectively:

- **EntityManager + JPQL** — inject `@PersistenceContext EntityManager`, write JPQL queries manually. Used for complex/dynamic queries (book search/filter/paginate).
- **Criteria API** — used specifically for `BookRepositoryImpl.findAllWithFilters()` to compose optional predicates type-safely without string concatenation.
- **`save` pattern** — `em.persist` for new entities (`id == null`), `em.merge` for detached ones.

---

## DTO Mapping Strategy

Static factory method on the response DTO — simple, co-located, no extra class:

```java
public class BookResponse {
    // fields + getters
    public static BookResponse from(Book book) { ... }
}
```

---

## Coding Style Conventions

1. JPA no-arg constructors are `protected` (not `public`) — not for general use
2. `@Transactional(readOnly = true)` on all read-only service methods
3. `@Valid` on all controller request DTOs with Bean Validation annotations
4. Explicit HTTP status codes: `201 CREATED` for POST, `204 NO CONTENT` for DELETE
5. `BigDecimal` for all monetary values — never `double`/`float`
6. `Instant` for all timestamps

---

## Phase 0 — Project Bootstrap

- [ ] Generate project on `start.spring.io` (Java 25, Gradle Kotlin DSL)
  - Dependencies: `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `postgresql`, `jjwt` (0.12.x), `spring-boot-starter-test`
- [ ] Configure `application.properties` — datasource via env vars (`${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`), JPA DDL, JWT secret/expiry (`${JWT_SECRET}`)
- [ ] Add `.env.example` listing all required env vars (actual `.env` is gitignored)
- [ ] Add `schema.sql` or single Flyway `V1__init.sql` for DDL (do not rely on `ddl-auto=create`)
- [ ] Add `Dockerfile`:
  - Multi-stage build — stage 1: `maven:3.9-eclipse-temurin-21` to build the jar; stage 2: `eclipse-temurin:21-jre` to run it
  - Expose port `8080`
- [ ] Add `docker-compose.yml`:
  - `db` service — `postgres:16-alpine`, named volume for data persistence, env vars from `.env`
  - `app` service — builds from `Dockerfile`, depends on `db`, passes env vars through, maps port `8080`
- [ ] Verify `docker compose up --build` starts both containers and app connects to DB
- [ ] Commit: `chore: bootstrap Spring Boot project with Docker setup`

---

## Phase 1 — Domain Entities

- [ ] `Role` enum — `ADMIN`, `USER`
- [ ] `OrderStatus` enum — `PENDING`, `SHIPPED`, `DELIVERED`, `CANCELLED`
- [ ] `User` — id, email (unique), passwordHash, firstName, lastName, role, createdAt, updatedAt
- [ ] `Category` — id, name (unique), description
- [ ] `Book` — id, title, author, description, price (`BigDecimal`), stock, coverImageUrl, category (`@ManyToOne LAZY`), deleted (soft-delete flag), createdAt, updatedAt
- [ ] `Cart` — id, user (`@OneToOne`), items (`@OneToMany cascade=ALL orphanRemoval=true`), createdAt
- [ ] `CartItem` — id, cart (`@ManyToOne`), book (`@ManyToOne`), quantity
- [ ] `Order` — id, user (`@ManyToOne`), items (`@OneToMany cascade=ALL`), status, totalPrice (`BigDecimal`), createdAt, updatedAt
- [ ] `OrderItem` — id, order (`@ManyToOne`), book (`@ManyToOne`), quantity, **priceAtPurchase** (`BigDecimal` — snapshot at order time)
- [ ] Add `@Version` on `Book` for optimistic locking on stock deduction
- [ ] Use `@PrePersist` / `@PreUpdate` lifecycle hooks for timestamps
- [ ] Verify schema generates cleanly on startup
- [ ] Commit: `feat: add JPA domain entities`

---

## Phase 2 — Security & Auth

- [ ] `UserRepository` interface + `UserRepositoryImpl` (EntityManager):
  - `findByEmail(String)`, `findById(Long)`, `save(User)`, `existsByEmail(String)`
- [ ] `UserDetailsServiceImpl` — load by email, map `Role` to `ROLE_` prefixed authority
- [ ] `JwtTokenProvider` — `generateToken`, `extractUsername`, `isTokenValid`; secret from `${JWT_SECRET}` env var; use jjwt 0.12.x API
- [ ] `JwtAuthenticationFilter extends OncePerRequestFilter` — parse Bearer token, set `SecurityContextHolder`; on invalid token clear context and continue (do not throw)
- [ ] `SecurityConfig` — lambda-DSL `SecurityFilterChain` (no `WebSecurityConfigurerAdapter`); stateless sessions; CSRF disabled; permit `/api/auth/**`; add `@EnableMethodSecurity`
- [ ] `GlobalExceptionHandler` (`@RestControllerAdvice`) + `ErrorResponse` record:
  - `ResourceNotFoundException` → 404
  - `DuplicateResourceException` → 409
  - `InsufficientStockException` → 409
  - `InvalidOperationException` → 400
  - `MethodArgumentNotValidException` → 400 with field-level errors
  - `AccessDeniedException` → 403
  - Fallback `Exception` → 500 (never leak stack traces)
- [ ] `AuthController POST /api/auth/register` — validate, hash (BCrypt), save, return JWT
- [ ] `AuthController POST /api/auth/login` — authenticate, return JWT
- [ ] Integration test: register → login → access protected endpoint
- [ ] Commit: `feat: JWT auth — register, login, security filter chain`

---

## Phase 3 — User Profile

- [ ] `AuthenticatedUserResolver` component — `getCurrentUser()` resolves from `SecurityContext` → `UserRepository`; inject into services (not controllers)
- [ ] `UserController GET /api/users/me` — return `UserResponse`
- [ ] `UserController PUT /api/users/me` — update firstName, lastName
- [ ] `UserController PATCH /api/users/me/password` — verify current password with `BCryptPasswordEncoder.matches`, then update
- [ ] Commit: `feat: user profile endpoints`

---

## Phase 4 — Categories

- [ ] `CategoryRepository` interface + impl — `findAll`, `findById`, `findByName`, `save`, `delete`
- [ ] `CategoryService` — CRUD with duplicate-name check
- [ ] `CategoryController /api/categories`:
  - `GET /` — public
  - `GET /{id}` — public
  - `POST /` — `@PreAuthorize("hasRole('ADMIN')")`
  - `PUT /{id}` — `@PreAuthorize("hasRole('ADMIN')")`
  - `DELETE /{id}` — `@PreAuthorize("hasRole('ADMIN')")`
- [ ] Commit: `feat: category management`

---

## Phase 5 — Books

- [ ] `BookRepository` interface + `BookRepositoryImpl` (EntityManager + Criteria API):
  - `findById`, `save`, `softDelete` (sets `deleted = true`)
  - `findAllWithFilters(title, author, categoryId, page, size)` — Criteria API with optional predicates
  - `countWithFilters(title, author, categoryId)` — parallel count query for `PageResponse`
- [ ] `BookService` — CRUD (admin), `searchBooks(...)` returning `PageResponse<BookResponse>`
- [ ] `PageResponse<T>` — hand-rolled generic pagination wrapper (content, page, size, totalElements, totalPages)
- [ ] `BookController /api/books`:
  - `GET /` — authenticated; query params `title`, `author`, `categoryId`, `page`, `size`
  - `GET /{id}` — authenticated
  - `POST /` — ADMIN only
  - `PUT /{id}` — ADMIN only
  - `DELETE /{id}` — ADMIN only (soft delete)
- [ ] `@DataJpaTest` for `BookRepositoryImpl.findAllWithFilters` with multiple filter combinations
- [ ] Commit: `feat: book management with dynamic search and pagination`

---

## Phase 6 — Shopping Cart

- [ ] `CartRepository` — `findByUserId`, `save`
- [ ] `CartItemRepository` — `findByCartIdAndBookId`, `save`, `delete`
- [ ] `CartService`:
  - `getCart(User)` — find or create cart
  - `addItem(User, bookId, quantity)` — increment if exists; stock NOT checked here (checked at order time)
  - `updateItem(User, bookId, quantity)`
  - `removeItem(User, bookId)` — remove from `cart.items` collection + merge (no explicit DELETE query)
  - `clearCart(User)`
- [ ] All service read methods `@Transactional(readOnly = true)`; use `JOIN FETCH` on cart query to avoid N+1 on CartItem → Book
- [ ] `CartController /api/cart` — all authenticated:
  - `GET /`
  - `POST /items`
  - `PUT /items/{bookId}`
  - `DELETE /items/{bookId}`
  - `DELETE /`
- [ ] Commit: `feat: shopping cart`

---

## Phase 7 — Orders

- [ ] `OrderRepository` — `findById`, `findByUserId` (DESC by createdAt), `findAll` (admin), `save`
- [ ] `OrderService.placeOrder(User)` — single `@Transactional` method:
  1. Load cart — throw `InvalidOperationException` if empty
  2. **Validation pass**: check `book.stock >= item.quantity` for ALL items — throw `InsufficientStockException` on first failure
  3. **Mutation pass**: deduct stock from each book, merge
  4. Create `Order` (status `PENDING`) + `OrderItem`s with `priceAtPurchase` snapshot
  5. `clearCart(user)`
  6. Return `OrderResponse`
- [ ] `OrderService.updateStatus(orderId, newStatus)` — ADMIN only; enforce valid transitions (no transition out of `CANCELLED`)
- [ ] `OrderController /api/orders`:
  - `POST /` — place order
  - `GET /my` — calling user's history
  - `GET /{id}` — own order (USER) or any order (ADMIN)
  - `GET /` — ADMIN only, all orders
  - `PATCH /{id}/status` — ADMIN only
- [ ] Unit test for `OrderService.placeOrder` — insufficient stock path + happy path
- [ ] Commit: `feat: order placement and management`

---

## Phase 8 — Polish

- [ ] Review `SecurityConfig` — ensure no endpoint gaps
- [ ] Verify all request DTOs have `@Valid` + Bean Validation annotations
- [ ] Verify all money fields are `BigDecimal`, all timestamps are `Instant`
- [ ] Add `@Operation` / `@Tag` Swagger annotations if `springdoc-openapi` is added
- [ ] Update README with final setup instructions and env var list
- [ ] Commit: `feat: validation and security hardening`

---

## Entity Relationships

```
User  ──(1:1)──  Cart  ──(1:N)──  CartItem  ──(N:1)──  Book
User  ──(1:N)──  Order ──(1:N)──  OrderItem ──(N:1)──  Book
Book  ──(N:1)──  Category
```

---

## Risk Areas

| Risk | Mitigation |
|---|---|
| Stock race condition (concurrent orders) | `@Version` on `Book` for optimistic locking |
| `LazyInitializationException` on CartResponse | `JOIN FETCH` in cart query inside `@Transactional(readOnly=true)` service |
| `TransactionRequiredException` in repositories | `@Transactional` only on service methods — never on repository |
| Spring Security 6 API (`WebSecurityConfigurerAdapter` removed) | Lambda-DSL `SecurityFilterChain` from day one |
| Order total wrong after book re-pricing | `priceAtPurchase` snapshot in `OrderItem` |
| Credentials committed | Env vars only; `application-local.yml` in `.gitignore` |

---

## Phase 9 — Database Migrations (Flyway)

- [ ] Add `flyway-core` dependency to `build.gradle.kts`
- [ ] Set `spring.flyway.enabled=true`, `spring.jpa.hibernate.ddl-auto=validate` in `application.yml`
- [ ] Create `src/main/resources/db/migration/V1__init.sql`:
  - DDL for all tables: `users`, `categories`, `books`, `carts`, `cart_items`, `orders`, `order_items`
  - Indexes: `users(email)`, `books(deleted)`, `orders(user_id)`, `cart_items(cart_id, book_id)`
- [ ] Create `V2__seed_admin.sql` — insert a default admin user (hashed password, role `ADMIN`) for local dev
- [ ] Verify clean `docker compose up --build` runs all migrations in order
- [ ] Add `spring.flyway.locations=classpath:db/migration` to `application.yml`
- [ ] Commit: `feat: add Flyway schema migrations`

---

## Phase 10 — Frontend (React + TypeScript)

### Setup

- [ ] Scaffold with Vite: `npm create vite@latest bookstore-ui -- --template react-ts`
- [ ] Place under `frontend/` in the repo root
- [ ] Add `axios` for HTTP, `react-router-dom` for routing, `zustand` for lightweight state
- [ ] Configure Vite proxy: `/api` → `http://localhost:8080` (avoids CORS in dev)
- [ ] Commit: `chore: scaffold React/TypeScript frontend`

### Auth

- [ ] `POST /api/auth/register` — Register form (email, password, firstName, lastName)
- [ ] `POST /api/auth/login` — Login form
- [ ] Store JWT in `localStorage`; attach as `Authorization: Bearer` header via Axios interceptor
- [ ] Protected route wrapper — redirect to `/login` if no token
- [ ] Commit: `feat: frontend auth — register, login, JWT handling`

### Books & Browse

- [ ] Book list page — search bar, category filter dropdown, price range inputs, sort controls
- [ ] Paginated results grid — book card (cover, title, author, price, "Add to Cart" button)
- [ ] Book detail page — full description, stock indicator, quantity selector
- [ ] Commit: `feat: frontend book browse and search`

### Cart

- [ ] Cart page — list items, quantity controls, remove button, total
- [ ] "Add to Cart" calls `POST /api/cart/items`; cart icon shows item count in nav
- [ ] Commit: `feat: frontend shopping cart`

### Checkout & Orders

- [ ] Checkout button on cart page → `POST /api/orders` → redirect to order confirmation
- [ ] Order history page — list user's orders with status badge
- [ ] Order detail page — items, prices, status
- [ ] Commit: `feat: frontend checkout and order history`

### Admin Panel

- [ ] Admin-only route guard (`role === 'ADMIN'`)
- [ ] Category management page — list, create, edit, delete
- [ ] Book management page — list, create, edit, soft-delete
- [ ] Order management page — list all orders, status update dropdown
- [ ] Commit: `feat: frontend admin panel`

### Polish

- [ ] Loading states and error messages on all forms
- [ ] 404 and empty-state pages
- [ ] Responsive layout (mobile-friendly)
- [ ] Commit: `feat: frontend polish — loading states, responsive layout`

---

## Post-Completion

- [ ] Add Testcontainers integration tests (PostgreSQL)
- [ ] Move plan to `plans/completed/`

---

## Phase 11 — Redis Caching

### Why Redis here

`GET /api/books` and `GET /api/books/{id}` hit PostgreSQL on every request. Book catalog data
changes rarely (only when an admin creates/updates/deletes a book). Redis caches responses
so repeated reads are served from memory instead of the database.

### What is cached

| Cache name | Key pattern | Evicted when |
|---|---|---|
| `books` | `search:<params>` | any book created / updated / deleted |
| `books` | `id:<id>` | any book created / updated / deleted |

`allEntries = true` on eviction is intentional: a stock change, price edit, or new book
invalidates all search result pages (they may contain the changed book).

### Implementation

- `build.gradle.kts` — add `spring-boot-starter-cache` + `spring-boot-starter-data-redis`
- `CacheConfig.java` — `RedisCacheManager` bean wired with `GenericJackson2JsonRedisSerializer`
  (JSON serialization — no need for `Serializable` on DTOs); TTL 5 minutes
- `BookstoreApplication` — `@EnableCaching`
- `BookService`:
  - `@Cacheable(value = "books", key = "...")` on `search(...)` and `getById(...)`
  - `@CacheEvict(value = "books", allEntries = true)` on `create`, `update`, `delete`
- `application.yml` — `spring.data.redis.host: ${REDIS_HOST:localhost}` (falls back to localhost in dev)

### Docker setup

`docker-compose.yml` runs `redis:7-alpine` with:
- `--appendonly yes` — AOF persistence so cache survives container restarts
- named volume `redis_data` for durability
- healthcheck via `redis-cli ping`
- `app` service depends on `redis` being healthy before starting

In dev without Docker, a local Redis on `localhost:6379` works with no extra config.

---

## Book Cover Images — Dev vs Prod Strategy

### How it works in dev (current)

Images are downloaded from Open Library and stored in:
```
src/main/resources/static/covers/{slug}.jpg
```
Spring Boot auto-serves everything under `static/` at the root path, so:
```
GET http://localhost:8080/covers/dune.jpg
```
returns the file with no extra controller code.

The `cover_image_url` column in the database stores the relative path:
```
/covers/dune.jpg
```
`BookResponse.coverImageUrl` returns this value. The React frontend renders:
```tsx
<img src={book.coverImageUrl} />         // resolves to http://localhost:8080/covers/dune.jpg
```

**V8 migration** (`V8__set_cover_image_urls.sql`) sets these paths for all 23 seeded books.

### How to switch to prod (S3 / CDN)

1. Upload all images from `src/main/resources/static/covers/` to an S3 bucket (or any CDN)
2. Write a Flyway migration (e.g. `V9__set_prod_cover_urls.sql`) that updates the DB values:
   ```sql
   UPDATE books SET cover_image_url = 'https://cdn.example.com/covers/dune.jpg' WHERE title = 'Dune';
   -- ...etc
   ```
3. The Spring Boot `static/` folder and the `cover_image_url` values in the DB are independent —
   changing the DB values to CDN URLs requires zero code changes; the frontend renders whatever
   URL the API returns.
4. Optionally remove `src/main/resources/static/covers/` from the Docker image to keep it lean.

### Why not a redirect endpoint

A `GET /api/books/{id}/cover` → `302` redirect would let you hide the storage backend from
the frontend, but adds an extra HTTP round-trip per image with no benefit at this scale.
The direct URL approach is what every major e-commerce platform uses once a CDN is in place.
