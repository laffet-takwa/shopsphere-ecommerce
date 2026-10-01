# ShopSphere

ShopSphere is a cloud-native portfolio storefront built as eight independently buildable Spring Boot services behind an API gateway. The Vue 3 client uses real REST endpoints; orders reserve stock and trigger simulated payments through Kafka events.

<p align="center">
   <img src="https://readme-typing-svg.demolab.com?font=Manrope&weight=600&size=24&duration=2800&pause=900&color=284C3D&center=true&vCenter=true&width=760&lines=Cloud-native+commerce%2C+thoughtfully+built;Eight+services.+One+event-driven+journey." alt="Cloud-native commerce, thoughtfully built" />
</p>

<p align="center">
   <img src="https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white" alt="Java 17" />
   <img src="https://img.shields.io/badge/Spring_Boot-3-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot 3" />
   <img src="https://img.shields.io/badge/Vue.js-3-4FC08D?logo=vuedotjs&logoColor=white" alt="Vue 3" />
   <img src="https://img.shields.io/badge/Kafka-event--driven-231F20?logo=apachekafka&logoColor=white" alt="Apache Kafka" />
   <img src="https://img.shields.io/badge/Docker_Compose-ready-2496ED?logo=docker&logoColor=white" alt="Docker Compose" />
</p>

<p align="center">
   <img src="https://images.unsplash.com/photo-1600210492486-724fe5c67fb0?auto=format&fit=crop&w=1500&q=85" alt="ShopSphere storefront hero: a calm, considered living space" width="900" />
</p>

## Architecture at a Glance

```mermaid
flowchart LR
   Web[Vue 3 storefront] --> Gateway[Spring Cloud Gateway]
   Gateway --> Auth[Auth service]
   Gateway --> Products[Product service]
   Gateway --> Orders[Order service]
   Gateway --> Inventory[Inventory service]
   Gateway --> Payments[Payment service]
   Gateway --> Notices[Notification service]
   Auth --> AuthDB[(PostgreSQL)]
   Products --> ProductDB[(PostgreSQL)]
   Orders --> OrderDB[(PostgreSQL)]
   Payments --> PaymentDB[(PostgreSQL)]
   Orders -->|order.created| Kafka[(Kafka)]
   Kafka --> Inventory
   Inventory -->|inventory.reserved| Kafka
   Kafka --> Payments
   Payments -->|payment.completed / payment.failed| Kafka
   Kafka --> Notices
   Inventory --> StockDB[(MongoDB)]
   Notices --> NoticeDB[(MongoDB)]
```

The order flow is intentionally visible here: order creation fans into inventory, payment, then customer notification, with service-owned storage along the way.

## Run Locally

Requirements: Docker Desktop with Compose, Node.js 20+, and a modern browser. Java 17 and Maven 3.9+ are needed only to build/run a backend service outside containers.

1. Copy `.env.example` to `.env` and replace the local values. For a quick local-only admin, set `SHOPSPHERE_ADMIN_EMAIL` and `SHOPSPHERE_ADMIN_PASSWORD` (at least 12 characters). The account is created once and receives the ADMIN role.
2. Start the stack from the repository root:

   ```powershell
   docker compose up --build
   ```

3. Open the storefront at [http://localhost:3000](http://localhost:3000/). The API is at port 8080, Eureka at 8761, Kibana at 5601, Elasticsearch at 9200, Kafka's host listener at 29092, PostgreSQL at 5432, and MongoDB at 27017.

The first startup builds eight Maven projects and downloads several images. Browse the seeded catalog without an account; register as a customer to place an order. Product and inventory writes require the configured admin JWT.

For direct local debugging of the backend ports, start with `docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build`.

The Compose password/JWT defaults are development-only. Do not use them outside a local sandbox. Compose runs single-node Kafka, databases, and Elasticsearch with security disabled, which is not a production topology.

## Services

| Service | Port | Data / responsibility |
| --- | ---: | --- |
| discovery-server | 8761 | Eureka registry |
| api-gateway | 8080 | Routing, JWT verification, role checks, identity headers, CORS |
| auth-service | 8081 | PostgreSQL users, BCrypt, JWT issue and validation |
| product-service | 8082 | PostgreSQL catalog and search |
| order-service | 8083 | PostgreSQL orders, `order.created`/`order.shipped` publisher, idempotent consumers |
| inventory-service | 8084 | MongoDB stock, reservations, `payment.failed` compensation |
| payment-service | 8085 | PostgreSQL simulated payments with a deterministic decline path |
| notification-service | 8086 | MongoDB event notifications |

Each backend directory has its own `pom.xml`. Build any one independently with `mvn -f backend/<service>/pom.xml verify`.

## Event Flow

`order.created` → inventory reservation → `inventory.reserved` → simulated payment → `payment.completed` → order becomes PAID → `order.shipped` once an administrator ships it. A short reservation publishes `inventory.insufficient`; a declined payment publishes `payment.failed`, which cancels the order and releases its reserved stock. Set `PAYMENT_DECLINE_ABOVE_AMOUNT` below an order total to watch the failure path run.

Consumers are idempotent: order-service persists each handled `eventId`, guarded status transitions reject late or duplicate messages, and reservations and payments are pinned per order.

## Repository Map

- `backend/`: eight independent Java 17 / Spring Boot projects.
- `frontend/shopsphere-web/`: Vue 3, TypeScript, Vite, Pinia, Axios, and Lucide.
- `infrastructure/`: Compose Dockerfiles, PostgreSQL initialization, Kafka topic provisioning, MongoDB indexes, and the ELK pipeline.
- `kubernetes/`: namespace-scoped application and infrastructure manifests.
- `docs/`: architecture, API, event, database, and deployment notes.
- `.github/workflows/ci.yml`: service tests and frontend production build.

## Known Portfolio Boundaries 

This is a runnable local reference architecture, not a claim of production readiness. It uses a single PostgreSQL container with one database per service, single-node Kafka without replication or a schema registry, synchronous catalog price lookups, and a simulated processor that declines deterministically above a configurable limit rather than talking to a real one. For production, replace schema auto-update with versioned migrations, add a transactional outbox so a database commit and its event cannot diverge, add distributed tracing and rate limiting, move secrets to a secret manager, and use redundant/secured infrastructure. See [deployment](docs/deployment.md).
