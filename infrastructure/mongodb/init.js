/*
 * ShopSphere MongoDB bootstrap.
 *
 * Runs once on an empty data volume via the Compose entrypoint hook. Spring Data Mongo is also
 * configured with auto-index-creation, so this script is belt-and-braces: it makes the required
 * indexes explicit and versioned in the repository rather than an implicit side effect of an
 * entity annotation.
 *
 * Both services own a separate database, in line with database-per-service.
 */

// eslint-disable-next-line no-undef
const inventory = db.getSiblingDB('shopsphere_inventory');

// One stock row per product; the unique index is what makes upserts race-safe.
inventory.inventory.createIndex({ productId: 1 }, { unique: true, name: 'uniq_inventory_product' });

// eslint-disable-next-line no-undef
const notifications = db.getSiblingDB('shopsphere_notifications');

// Backs GET /api/notifications, which always filters by user and orders newest first.
notifications.notifications.createIndex({ userId: 1, createdAt: -1 }, { name: 'notifications_by_user' });

// Backs the idempotency check that stores each Kafka event exactly once.
notifications.notifications.createIndex({ eventId: 1 }, { unique: true, name: 'uniq_notification_event' });