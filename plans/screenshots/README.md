# UI Screenshots

Screenshots of the React frontend running against the seeded dev backend
(`http://localhost:5173`, proxying `/api` → `:8080`).

| File | Page | Route | Notes |
|------|------|-------|-------|
| [`01-books-browse.png`](01-books-browse.png) | Catalog browse | `/books` | Search, category + price filters, sort, book grid with Cloudinary covers (logged in as *Pesho*, USER) |
| [`02-book-detail.png`](02-book-detail.png) | Book detail | `/books/15` | Cover, author, category badge, price, quantity stepper, *Add to Cart* |
| [`03-shopping-cart.png`](03-shopping-cart.png) | Shopping cart | `/cart` | Line items, quantity controls, remove, running total, *Checkout* |
| [`04-order-detail.png`](04-order-detail.png) | Order detail | `/orders/1` | Order status badge, placed-at timestamp, line items with price snapshot, total |
| [`05-admin-books.png`](05-admin-books.png) | Admin — Books | `/admin/books` | New-book form + book table with edit / delete / cover upload (logged in as *Rumen*, ADMIN) |
| [`06-admin-orders.png`](06-admin-orders.png) | Admin — Orders | `/admin/orders` | All orders with customer attribution, status, and transition buttons |

Captured against the `dev`-profile seed data (6 categories, 23 books, demo
users). See [`../../README.md`](../../README.md) for the feature overview and
[`../frontend-ui-guide.md`](../frontend-ui-guide.md) for the page-by-page guide.
