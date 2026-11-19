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
| Credentials committed | Env vars only; `application-local.properties` in `.gitignore` |

---

## Post-Completion

- [ ] Add Flyway for proper schema migrations (if started with `schema.sql`)
- [ ] Add Testcontainers integration tests (PostgreSQL)
- [ ] Add `springdoc-openapi` for Swagger UI
- [ ] Move plan to `plans/completed/`
