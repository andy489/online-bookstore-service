# Frontend UI Guide

> React + TypeScript SPA for the Online Bookstore. Talks to the Spring Boot backend via REST API.

---

## Stack

| Layer | Technology |
|---|---|
| Framework | React 19 + TypeScript (Vite) |
| Routing | react-router-dom v7 |
| State | Zustand (auth + cart — persisted in localStorage) |
| HTTP | Axios with Bearer JWT interceptor |
| Build | Vite 8 |
| Docker | nginx:1.27-alpine (serves `dist/`, proxies `/api` to `app:8080`) |

---

## Development Workflow

### Prerequisites
- Node 22+
- Java 21+
- Docker (for MySQL + Redis)

### Step 1 — Start infra (MySQL + Redis)

```bash
docker compose up
```

### Step 2 — Start the Spring Boot backend

```bash
./gradlew bootRun
```

Backend runs at **http://localhost:8080**

### Step 3 — Start the frontend dev server

```bash
cd frontend
npm run dev
```

Frontend runs at **http://localhost:5173** (proxies `/api` → `localhost:8080`)

### Seeded accounts (all password `1234`)

| Email | Role | Access |
|---|---|---|
| `pesho@gmail.com` | USER | Browse, cart, orders, profile |
| `anya@bookstore.local` | USER | Browse, cart, orders, profile |
| `rumen@bookstore.local` | ADMIN | Everything + Admin panel |

---

## Full Docker Compose (self-contained demo)

Builds and runs all four services:

```bash
cp .env.example .env     # fill in DB_USERNAME, DB_PASSWORD, JWT_SECRET, Cloudinary creds
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend (nginx) | http://localhost |
| Backend (Spring Boot) | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

The nginx container serves the React build and proxies `/api` → `app:8080`. The browser only ever talks to port 80.

---

## Page Reference

### Public (unauthenticated)

| Path | Page |
|---|---|
| `/login` | Sign-in form |
| `/register` | Registration form |

---

### User (any authenticated user)

#### `/books` — Browse Books

- Search bar (title / author keyword)
- Category filter dropdown
- Min/max price inputs
- Sort: Title A–Z/Z–A, Price Low–High/High–Low, Newest
- 12-per-page grid with Cloudinary cover images
- "Add to Cart" button per book; shows "✓ Added" briefly after success
- Out-of-stock books show a grey badge instead of the button

#### `/books/:id` — Book Detail

- Full cover image, title, author, category, price, description
- Stock count + quantity selector (capped to available stock)
- "Add to Cart" button

#### `/cart` — Shopping Cart

- Lists all items with cover thumbnail, title, author
- Quantity increment/decrement buttons (live update)
- Remove item per row
- "Clear Cart" clears everything
- Running total + "Checkout" button

#### `/checkout` — Checkout

- Review summary of all items and total price
- "Place Order" → calls `POST /api/orders`
- On success: confirmation page showing Order ID, total, and any **partial-fulfillment adjustments** (e.g. "Dune: requested 3, fulfilled 2 — Insufficient stock")
- Buttons to view orders or continue shopping

#### `/orders` — My Orders

- List of all past orders (newest first)
- Status badge: PENDING / SHIPPED / DELIVERED / CANCELLED
- Click any row to open order detail

#### `/orders/:id` — Order Detail

- Items with quantity and price-at-purchase snapshot
- Order total and status

#### `/profile` — Profile

- Update first name / last name
- Change password (invalidates the current session token — you will be redirected to login)

---

### Admin panel (ADMIN role only)

Access via the **Admin ▾** dropdown in the navbar.

#### `/admin/books` — Book Management

- Create a new book (title, author, description, price, stock, category)
- Edit existing books inline
- Soft-delete a book (removes from browse but preserves order history)
- Upload a cover image per book → uploads to Cloudinary CDN, URL stored in DB
- Paginated table (10 per page)

#### `/admin/categories` — Category Management

- Create / edit / delete categories
- Used as the filter dropdown on the browse page

#### `/admin/orders` — Order Management

- All orders across all users (15 per page)
- Status transition buttons based on the allowed state machine:
  - PENDING → SHIPPED or CANCELLED
  - SHIPPED → DELIVERED or CANCELLED
  - DELIVERED and CANCELLED have no further transitions
- "View" link opens the shared `/orders/:id` detail page

---

## Auth Flow

1. Register or login → backend issues a JWT (24h default)
2. JWT stored in `localStorage` via Zustand persist
3. Every Axios request attaches `Authorization: Bearer <token>`
4. On 401 response → Zustand clears state, redirects to `/login`
5. Logout → calls `POST /api/auth/logout` (blocklists the JWT server-side) then clears local state
6. Password change → backend invalidates all prior tokens via the `tva` claim; the calling token is immediately blocklisted; user must log in again

---

## Key Files

```
frontend/
├── src/
│   ├── api.ts                    # Axios instance + JWT interceptor
│   ├── types.ts                  # All TypeScript interfaces
│   ├── App.tsx                   # Router + route definitions
│   ├── stores/
│   │   ├── authStore.ts          # Zustand auth (persisted)
│   │   └── cartStore.ts          # Zustand cart (synced with backend)
│   ├── components/
│   │   ├── Navbar.tsx            # Top nav with cart count + admin dropdown
│   │   └── RequireAuth.tsx       # Route guard (also adminOnly flag)
│   └── pages/
│       ├── LoginPage.tsx
│       ├── RegisterPage.tsx
│       ├── BooksPage.tsx
│       ├── BookDetailPage.tsx
│       ├── CartPage.tsx
│       ├── CheckoutPage.tsx
│       ├── OrdersPage.tsx
│       ├── OrderDetailPage.tsx
│       ├── ProfilePage.tsx
│       ├── NotFoundPage.tsx
│       └── admin/
│           ├── AdminBooksPage.tsx
│           ├── AdminCategoriesPage.tsx
│           └── AdminOrdersPage.tsx
├── nginx.conf                    # nginx proxy config for Docker
├── Dockerfile                    # Multi-stage: node build → nginx serve
└── vite.config.ts                # Vite + /api proxy for dev
```

---

## Docker Services

```
db      mysql:8.0        port 3306
redis   redis:7-alpine   port 6379
```

The Spring Boot app and React frontend run locally (not in Docker).

| Service | How to run | URL |
|---|---|---|
| Infra (MySQL + Redis) | `docker compose up` | — |
| Backend | `./gradlew bootRun` | http://localhost:8080 |
| Frontend | `cd frontend && npm run dev` | http://localhost:5173 |
| Swagger UI | (backend must be running) | http://localhost:8080/swagger-ui.html |
