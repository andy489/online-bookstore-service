# Book Cover — Cloudinary URL Mapping

All 23 book covers are hosted on Cloudinary under the `bookstore/covers/` folder in the `dmpzzernt` account.

## Where the URLs live in code

The cover URLs are seeded directly in the books INSERT statements inside:

```
src/main/resources/db/migration/V3__seed_catalog.sql
```

A copy of this file also exists under `src/main/resources/db/seed/V3__seed_catalog.sql` — that copy is used only when the `dev` Spring profile is active (via `application-dev.yml`). To change a seeded cover URL, update `db/migration/V3__seed_catalog.sql` (which runs in all environments). The `db/seed/` copy is only relevant for a bare dev setup that skips the standard migration.

Each book row includes a `cover_image_url` column set to its Cloudinary CDN URL as part of the INSERT — there is no separate UPDATE pass. Example:

```sql
INSERT INTO books (title, author, description, price, stock, cover_image_url, category_id, deleted, version, created_at, updated_at)
VALUES ('The Brothers Karamazov',
        'Fyodor Dostoevsky',
        '...',
        14.99, 40,
        'https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/brothers-karamazov.jpg',
        (SELECT id FROM categories WHERE name = 'Classic Literature'),
        FALSE, 0, NOW(), NOW()),
```

If you need to change a cover URL, update the matching row in `V3__seed_catalog.sql` (`db/migration/`), drop and recreate the database (`docker-compose down -v && docker-compose up -d`), and restart the app.

## Cover cleanup on update

When an admin updates a book via `PUT /api/books/{id}` and the `coverImageUrl` field changes to a new Cloudinary URL, `BookService.update()` automatically deletes the old Cloudinary asset (best-effort — failure is logged, not propagated) before saving the new URL. This prevents orphaned assets from accumulating in the `bookstore/covers/` folder.

---

## URL pattern

```
https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/{slug}.jpg
```

All slugs follow kebab-case. The full table below maps each book title to its slug and full URL.

---

## Classic Literature

| Book Title | Slug | Cloudinary URL |
|---|---|---|
| The Brothers Karamazov | `brothers-karamazov.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/brothers-karamazov.jpg) |
| Crime and Punishment | `crime-and-punishment.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/crime-and-punishment.jpg) |
| The Idiot | `the-idiot.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/the-idiot.jpg) |
| The Master and Margarita | `master-and-margarita.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/master-and-margarita.jpg) |
| Brave New World | `brave-new-world.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/brave-new-world.jpg) |

## War & Historical Fiction

| Book Title | Slug | Cloudinary URL |
|---|---|---|
| Catch-22 | `catch-22.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/catch-22.jpg) |
| Shogun | `shogun.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/shogun.jpg) |
| King Rat | `king-rat.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/king-rat.jpg) |
| Tai-Pan | `tai-pan.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/tai-pan.jpg) |
| For Whom the Bell Tolls | `for-whom-the-bell-tolls.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/for-whom-the-bell-tolls.jpg) |
| All Quiet on the Western Front | `all-quiet-western-front.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/all-quiet-western-front.jpg) |

## Crime & Mystery

| Book Title | Slug | Cloudinary URL |
|---|---|---|
| The Girl with the Dragon Tattoo | `girl-with-dragon-tattoo.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/girl-with-dragon-tattoo.jpg) |
| And Then There Were None | `and-then-there-were-none.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/and-then-there-were-none.jpg) |
| The Name of the Rose | `name-of-the-rose.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/name-of-the-rose.jpg) |
| Mystic River | `mystic-river.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/mystic-river.jpg) |
| Gone Baby Gone | `gone-baby-gone.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/gone-baby-gone.jpg) |
| Shutter Island | `shutter-island.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/shutter-island.jpg) |

## Philosophy & Drama

| Book Title | Slug | Cloudinary URL |
|---|---|---|
| The Stranger | `the-stranger.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/the-stranger.jpg) |
| Thus Spoke Zarathustra | `thus-spoke-zarathustra.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/thus-spoke-zarathustra.jpg) |
| Nausea | `nausea.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/nausea.jpg) |

## Epic & Adventure

| Book Title | Slug | Cloudinary URL |
|---|---|---|
| Dune | `dune.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/dune.jpg) |
| The Count of Monte Cristo | `count-of-monte-cristo.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/count-of-monte-cristo.jpg) |
| Don Quixote | `don-quixote.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/don-quixote.jpg) |
| One Hundred Years of Solitude | `one-hundred-years.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/one-hundred-years.jpg) |

## Science & Non-Fiction

| Book Title | Slug | Cloudinary URL |
|---|---|---|
| The Selfish Gene | `the-selfish-gene.jpg` | [link](https://res.cloudinary.com/dmpzzernt/image/upload/bookstore/covers/the-selfish-gene.jpg) |
