# MongoDB

Inventory and notification storage for ShopSphere, kept in separate databases so each service owns
its own documents:

| Database | Owner | Collection | Purpose |
| --- | --- | --- | --- |
| `shopsphere_inventory` | inventory-service | `inventory` | One stock row per product, plus a per-order reservation map |
| `shopsphere_notifications` | notification-service | `notifications` | Event-shaped notification documents, one per consumed Kafka event |

## Index strategy

Indexes are declared **only** as Spring Data annotations on the document classes
(`@Indexed`, including `unique = true`), and both services run with
`spring.data.mongodb.auto-index-creation: true`. MongoDB then creates them on first start.

This directory intentionally ships no `init.js`. An earlier version created the same indexes from a
`docker-entrypoint-initdb.d` hook, and the two owners collided: MongoDB refuses a second index over
the same key pattern under a different name
(`IndexOptionsConflict: 'Index already exists with a different name'`), which made both services
fail at context startup. Keeping one owner removes that class of failure entirely.

If you ever do add an init hook, either reuse the exact names Spring Data generates
(`<field>_1`, `<field>1_desc`) or disable `auto-index-creation` for that service so exactly one
component provisions indexes.

## Required indexes

`inventory`:

- `productId` — unique. Makes upserts race-safe and `findByProductId` an index seek.

`notifications`:

- `userId, createdAt` — compound. Backs `GET /api/notifications`, which filters by user and sorts
  newest first.
- `eventId` — unique. Backs the idempotency check that stores each Kafka event exactly once.

## Seed data

There is none. `InventoryBootstrap` creates a stock row for each catalog product the first time
inventory needs it, so a fresh volume converges on a usable dataset without any seeding step.
