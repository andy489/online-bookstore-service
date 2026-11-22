# Database Diagrams

Entity–relationship diagrams for the Online Bookstore schema.

| File | Description |
|------|-------------|
| [`bookstore-erd.png`](bookstore-erd.png) | ER diagram (raster, quick preview) |
| [`bookstore-erd.pdf`](bookstore-erd.pdf) | ER diagram (vector, print/zoom) |

## Source

Generated with DBeaver by reverse-engineering the live `bookstore` MySQL
schema (reachable at `localhost:3306` when the docker-compose `db` service is
up). The schema itself is defined in
[`../../src/main/resources/db/migration/V1__baseline_schema.sql`](../../src/main/resources/db/migration/V1__baseline_schema.sql).

## Regenerating

1. Start the database: `docker compose up -d db`
2. In DBeaver, connect to `localhost:3306` / db `bookstore` and expand
   **Databases ▸ bookstore ▸ Tables**.
3. Double-click **Tables** (or right-click the `bookstore` database ▸
   **View Diagram**) to open the **Diagram** tab.
4. Export via the diagram's bottom toolbar **Save as image** button, or
   right-click the canvas ▸ **Save Diagram as Image…**.

> `flyway_schema_history` is Flyway's bookkeeping table, not part of the
> domain model — it can be removed from the diagram for a cleaner view.
