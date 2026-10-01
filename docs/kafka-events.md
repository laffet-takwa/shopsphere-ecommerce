# Kafka Events

Events are JSON DTOs keyed by `orderId`, and each carries an `eventId` and UTC `occurredAt`. Internal JPA entities and MongoDB documents are never published directly, and producers emit plain JSON with no Java class name in the headers (`spring.json.add.type.headers: false`), so a consumer always maps a payload into its own event record rather than trusting a type from the wire.

| Topic | Producer | Consumer | Event purpose |
| --- | --- | --- | --- |
| `order.created` | order-service | inventory-service, notification-service, order-service | Order items and amount request stock reservation |
| `inventory.reserved` | inventory-service | payment-service, notification-service | Stock was reserved; trigger simulated payment |
| `inventory.insufficient` | inventory-service | order-service, notification-service | Reservation failed and the order is cancelled |
| `payment.completed` | payment-service | order-service, notification-service | Simulated payment succeeded; the order becomes PAID |
| `payment.failed` | payment-service | order-service, inventory-service, notification-service | Payment declined; the order is cancelled and reserved stock is released |
| `order.shipped` | order-service | notification-service | Shipping update |

All six topics are provisioned explicitly by `infrastructure/kafka/create-topics.sh` (Compose) and the `kafka-create-topics` Job (Kubernetes) with three partitions. Partitioning by event type is safe because every producer keys on the order ID, so all events for one order share a partition and stay ordered.

## Flow

1. Order service resolves product details and prices in one catalog call per line, persists the order and its items, then publishes `order.created`.
2. Inventory reserves every requested quantity in MongoDB. If any line fails, earlier reservations from the same attempt are released and `inventory.insufficient` is published; otherwise `inventory.reserved` is emitted.
3. Payment service persists one simulated payment per order and publishes `payment.completed` or `payment.failed`.
4. Order service reacts to `payment.completed` (mark PAID), `inventory.insufficient` (cancel), and `payment.failed` (cancel).
5. Inventory reacts to `payment.failed` and releases whatever is still held for that order, so a declined payment cannot leak a reservation.
6. An administrator can move a paid order to SHIPPED with `PUT /api/orders/{id}/ship`, which publishes `order.shipped`.
7. Notification service persists each event once by its unique event ID and exposes it to the owning user.

## Idempotency

A redelivered record must not apply the same effect twice.

- **order-service** stores every handled `eventId` in `processed_events` in the same transaction as the aggregate update, so a replay is ignored.
- **Status transitions are guarded.** `markPaid`, `cancel`, and `ship` return whether the aggregate actually changed; a late `payment.completed` cannot revive an order that was already cancelled.
- **inventory-service** treats a reservation for an order it already holds as a no-op, and releases are idempotent.
- **payment-service** pins one payment per order through a unique constraint on `order_id`.
- **notification-service** relies on a unique index on `eventId`.

## Failure handling

Listeners retry four times with exponential backoff and route exhausted records to retry/DLT topics created by Spring Kafka. A malformed payload therefore fails all attempts and lands in the DLT instead of blocking the partition.

This local reference has no automated DLT replay UI and no transactional outbox: the payment row commits before its event is published, so a crash in between loses the notification but not the charge. Closing that gap needs an outbox table plus a relay, which together with versioned event contracts, a schema registry, and metrics on retries/lag is the natural production evolution.