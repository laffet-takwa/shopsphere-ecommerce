# API

All browser calls use the gateway at `http://localhost:8080`. JSON request/response bodies use camelCase. Validation errors are returned by Spring MVC; unauthenticated and unauthorized gateway requests return 401 and 403 respectively.

## Authentication

| Method | Path | Access |
| --- | --- | --- |
| POST | `/api/auth/register` | Public: `firstName`, `lastName`, `email`, `password` |
| POST | `/api/auth/login` | Public: `email`, `password` |
| GET | `/api/auth/me` | Bearer JWT |

Register/login return `{ accessToken, tokenType, user }`. Passwords are never returned.

## Products

| Method | Path | Access |
| --- | --- | --- |
| GET | `/api/products?page=0&size=24&sort=createdAt,desc` | Public |
| GET | `/api/products?category=Home&keyword=lamp` | Public, filterable |
| GET | `/api/products/category/{category}` | Public |
| GET | `/api/products/search?keyword=lamp` | Public |
| GET | `/api/products/{id}` | Public |
| POST | `/api/products` | ADMIN |
| PUT | `/api/products/{id}` | ADMIN |
| DELETE | `/api/products/{id}` | ADMIN |

Create/update product bodies contain `name`, `description`, `price`, `category`, `sku`, optional `imageUrl`, and `active`.

## Orders, Inventory, Payments, Notifications

| Method | Path | Access |
| --- | --- | --- |
| POST | `/api/orders` | Bearer JWT; body `{ "items": [{ "productId": 1001, "quantity": 1 }] }` |
| GET | `/api/orders` | Bearer JWT; current user's orders |
| GET | `/api/orders/{id}` | Bearer JWT; owner only |
| GET | `/api/orders/user/{userId}` | Owner or ADMIN |
| PUT | `/api/orders/{id}/cancel` | Owner; pending orders only |
| PUT | `/api/orders/{id}/ship` | ADMIN; paid or processing orders only; publishes `order.shipped` |
| GET | `/api/inventory/{productId}` | Bearer JWT |
| PUT | `/api/inventory/{productId}` | ADMIN; body `{ "sku": "...", "quantity": 12 }` |
| POST | `/api/inventory/release` | ADMIN; body `{ "productId": 1001, "orderId": 45 }` |
| POST | `/api/inventory/reserve` | ADMIN; body `{ "productId": 1001, "orderId": 45, "quantity": 1 }` |
| POST | `/api/payments/process` | Owner or ADMIN; body `{ "orderId": 45 }`; returns the idempotently persisted result after inventory-triggered processing |
| GET | `/api/payments/{orderId}` | Owner or ADMIN |
| GET | `/api/notifications` | Bearer JWT; current user |
| PUT | `/api/notifications/{id}/read` | Bearer JWT; owner only |

An order request returns a PENDING order immediately. Inventory reservation and payment complete asynchronously; notifications report subsequent outcomes.

## Simulated payment failures

`shopsphere.payment.decline-above-amount` (env `PAYMENT_DECLINE_ABOVE_AMOUNT`, default `10000`) makes the simulated processor decline any order above that amount. The decline is deterministic, so it is reproducible in a demo and assertable in a test. A decline publishes `payment.failed`, which cancels the order and releases its reserved stock. Lower the value below a test order total to observe the failure path.