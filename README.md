# Online Bookstore Service

A RESTful backend for an online bookstore where users can browse, search, and purchase books. Admins manage inventory, categories, and orders.

## Task Description

Build a RESTful backend for an online bookstore, where users can browse, search, and purchase books. Admins manage inventory, categories, and orders.

### Core Features

#### 1. User Management

- **Register / Login**
  - Roles: `ADMIN`, `USER`
- **Profile Management**
  - View/edit profile
  - Change password

#### 2. Book Management

- **CRUD Operations (Admin only)**
  - Book fields: `title`, `author`, `description`, `price`, `stock`, `category`, `coverImageUrl`
- **View Books (User)**
  - List, filter, pagination
  - Search by title, author, category

#### 3. Category Management

- Admin can create/update/delete categories
- Users can filter books by category

#### 4. Shopping Cart

- Each user has a cart
- Add/remove books to/from cart
- Update quantity

#### 5. Order Management

- **Place Order**
  - Requires items in cart
  - Deduct stock from books
- **Order History**
  - Admin can view all orders
- Order statuses: `PENDING` → `SHIPPED` → `DELIVERED` / `CANCELLED`

### Notes

- The requirements are intentionally open-ended — feel free to interpret them
- Not all features need to be implemented; the goal is to show project structure and coding style
- No Lombok — plain Java code preferred
- No `CrudRepository` auto-generated queries — custom repository implementations preferred

## Implementation Plan

The project is built in phases, each independently committable:

| Phase | Scope |
|-------|-------|
| 0 | Project bootstrap — Spring Boot 3, dependencies, DB config, Docker setup |
| 1 | Domain entities — User, Book, Category, Cart, CartItem, Order, OrderItem |
| 2 | Security & Auth — JWT filter, register/login endpoints |
| 3 | User profile — view, edit, change password |
| 4 | Category management — admin CRUD, public list |
| 5 | Book management — admin CRUD, public search/filter/paginate |
| 6 | Shopping cart — add/remove/update items |
| 7 | Orders — place from cart (stock deduction), history, status management |
| 8 | Polish — global error handling, input validation, security hardening |

Full task breakdown: [`plans/bookstore-backend-implementation.md`](plans/bookstore-backend-implementation.md)

### Key Design Decisions

- **No Lombok** — plain Java getters/setters/constructors
- **No `CrudRepository`** — custom repositories using `EntityManager` (JPQL) or `Repository<T,ID>` with `@Query`
- **Layered architecture** — `controller → service → repository → domain`
- **Stateless JWT** — no server-side sessions
- **Stock deduction** is transactional with optimistic locking (`@Version` on `Book`) to handle concurrent orders
- **Docker** — app + MySQL + Redis containerized via Docker Compose for zero-setup local development

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Security + JWT
- Spring Data JPA + Flyway
- MySQL 8 (schema) + Redis 7 (cache / token blocklist)
- React + TypeScript + Vite (frontend)
- Gradle (Kotlin DSL)
- Docker + Docker Compose

## Getting Started

### Prerequisites

- Docker + Docker Compose

### Local development setup (recommended)

The most common dev workflow — PostgreSQL runs in Docker, the app runs locally for fast feedback and easy debugging:

```bash
git clone https://github.com/andy489/online-bookstore-service.git
cd online-bookstore-service

# Create local config file with your credentials
cp src/main/resources/application-local.yml.example \
   src/main/resources/application-local.yml
# Edit application-local.yml and fill in your JWT secret

docker compose up db        # start only the database
./gradlew bootRun --args='--spring.profiles.active=local'   # separate terminal
```

The API will be available at `http://localhost:8080`.

### Run everything in Docker

Runs both the app and the database in containers — no local JDK needed:

```bash
cp .env.example .env
docker compose up --build
```

### Run locally (no Docker)

**Prerequisites:** Java 21+, Gradle 9+, MySQL 8 running locally

```bash
cp .env.example .env        # fill in DB credentials and JWT_SECRET
./gradlew bootRun
```

## API Overview

| Resource   | Endpoint prefix   |
|------------|-------------------|
| Auth       | `/api/auth`       |
| Users      | `/api/users`      |
| Books      | `/api/books`      |
| Categories | `/api/categories` |
| Cart       | `/api/cart`       |
| Orders     | `/api/orders`     |

## Database Schema

The schema is defined in a single Flyway baseline migration:
[`src/main/resources/db/migration/V1__baseline_schema.sql`](src/main/resources/db/migration/V1__baseline_schema.sql).
Dev-only seed data lives under
[`src/main/resources/db/seed/`](src/main/resources/db/seed) and is applied only
with the `dev` profile.

Entity–relationship diagram:

![Bookstore ER diagram](plans/diagrams/bookstore-erd.png)

Higher-resolution and print versions, plus regeneration steps, are in
[`plans/diagrams/`](plans/diagrams/).
