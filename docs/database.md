# Database Design

The local PostgreSQL server is shared as infrastructure only. Auth, catalog, orders, and payments use separate databases and credentials can be split when deployed independently.

| Database / collection | Owner | Main records and constraints |
| --- | --- | --- |
| `shopsphere_auth.users` | auth-service | Unique case-insensitive application email, BCrypt hash, role, enabled, created timestamp |
| `shopsphere_products.products` | product-service | Unique SKU, positive `numeric(12,2)` price validation, category, active, timestamps |
| `shopsphere_orders.orders` / `order_items` | order-service | Order status, user ID, total; item foreign key with cascade, immutable product-name/price snapshot |
| `shopsphere_orders.processed_events` | order-service | Unique event ID and topic, used to make Kafka consumers idempotent |
| `shopsphere_payments.payments` | payment-service | Unique order ID and transaction reference, currency, status, amount |
| `shopsphere_inventory.inventory` | inventory-service | Unique indexed product ID, on-hand/reserved quantity, per-order reservation map |
| `shopsphere_notifications.notifications` | notification-service | Indexed user ID, unique event ID, message, read flag, creation time |

Foreign keys do not cross service databases. Order items snapshot catalog labels/prices to preserve order history. Inventory available stock is `quantity - reservedQuantity`; reservation IDs make duplicate order events harmless for the same item. Notifications use MongoDB because they are event-shaped documents and keep notification storage independent from transactional order/payment data.

MongoDB indexes are declared both as entity annotations and in `infrastructure/mongodb/init.js`, which runs once on an empty volume: a unique index on `inventory.productId`, and on notifications a compound `{ userId, createdAt }` index for the listing query plus a unique index on `eventId` for idempotency.

The local projects use Hibernate `ddl-auto: update` for quick setup and SQL bootstrap data for the catalog. Replace this with Flyway/Liquibase migrations, review indexes against real query plans, and use separate PostgreSQL instances/roles for production.