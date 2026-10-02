# Demo Script

A 6–8 minute walkthrough, scene by scene, with what to show and what to say. Everything below was
run and verified on this machine, so each beat is reproducible.

I cannot record the video myself — no screen capture here. Record it with OBS, ShareX, Xbox Game Bar
(`Win+Alt+R`) or Loom, and follow the cues.

## Before you record

```bash
docker compose up -d          # 14 containers
docker compose ps             # expect all running
curl -s localhost:9200/_cluster/health?pretty
curl -s localhost:5601/api/status | head -c 80
```

Open tabs before you start, so no cold start lands on camera:

| Tab | URL |
| --- | --- |
| Storefront | http://localhost:3001 |
| API gateway health | http://localhost:8080/actuator/health |
| Eureka | http://localhost:8761 |
| Kibana | http://localhost:5601 |
| Terminal | ready for the curl beats |

**Credentials.** The admin is bootstrapped from `.env`:
`SHOPSPHERE_ADMIN_EMAIL` (currently `admin@shopsphere.local`) and `SHOPSPHERE_ADMIN_PASSWORD`.
Read them rather than typing from memory on camera. Customers can self-register at `/register`.

---

## Scene 1 — The problem (0:00–0:40)

> "Most e-commerce portfolio projects are one Spring Boot file with an in-memory list. This one is
> eight independently deployable services. Let me show you the storefront first, then take it apart."

**Show:** the homepage at 1440×1000. Scroll the hero and the trust bar.

**Say:** the tagline, and that the catalogue is real data from PostgreSQL — hover a product to
prove the wishlist and add-to-cart work.

---

## Scene 2 — The topology (0:40–1:30)

Switch to Eureka at :8761. This is the strongest single frame in the demo — pause on it.

> "Eight services, all registered here. Nothing is hardcoded: the gateway resolves `order-service`
> by name through Eureka, and order-service discovers `product-service` the same way."

**Point at:** `API-GATEWAY`, `ORDER-SERVICE`, `INVENTORY-SERVICE` in the registry.
**Then:** terminal, `curl -s localhost:8761/eureka/apps | grep -o '"name":"[A-Z-]*"' | sort -u`.

---

## Scene 3 — The money shot: one order, four services (1:30–3:30)

This is the core of the project. Do it live, not as a recording.

Register a customer at `/register`, add two products, check out. Then immediately switch to the
terminal and watch the order move through the chain:

```bash
# watch the order transition without reloading anything
watch -n1 "curl -s -H \"Authorization: Bearer $TOKEN\" localhost:8080/api/orders/9 | jq -r .status"
```

**Say, in this order:**

1. "The order service writes the order and publishes `order.created`. It returns immediately —
   it does **not** wait for payment."
2. "Inventory reserves both lines in MongoDB, all-or-nothing. If one line is short it releases the
   earlier ones and publishes `inventory.insufficient`."
3. "Payment only runs once stock is secured. Money is never taken before the reservation."
4. "Order service consumes `payment.completed` and moves the order to PAID."

**Prove the idempotency guard** — this is the line that separates a real system from a demo:

```bash
docker compose exec order-service sh -c \
  "psql \$DATABASE_URL -c 'select event_id, topic, processed_at from processed_events order by id desc limit 3'"
```

> "Every consumed event is recorded with a unique ID in the same transaction as the aggregate
> update. Redeliver a message and it is dropped instead of double-charging."

**Then show compensation** — decline a payment and watch stock come back:

```bash
# lower the limit below the order total so the processor declines
PAYMENT_DECLINE_ABOVE_AMOUNT=1 docker compose up -d --force-recreate payment-service
# place an order, wait, then:
docker compose exec inventory-service mongosh shopsphere_inventory \
  --eval 'db.inventory.findOne({productId:1004}).reservedQuantity'
```

`payment.failed` cancels the order *and* releases the reservation. Say that explicitly — it is the
part most candidates cannot talk about.

---

## Scene 4 — Resilience (3:30–4:20)

```bash
docker compose exec order-service sh -c \
  "/opt/kafka/bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list | grep service"
```

> "Four consumers, each with a dead-letter consumer group. Every listener retries four times with
> exponential backoff, and a message that still fails lands in a DLT instead of blocking the
> partition."

**Say the limitation out loud:** there is no automated DLT replay UI and no transactional outbox, so
a crash between the database commit and the publish loses the notification but never double-charges.
Volunteering that is worth more than hiding it.

---

## Scene 5 — Observability (4:20–5:10)

Switch to Kibana. Create the data view on first open: `shopsphere-logs-*`, time field `@timestamp`.

Show Discover filtered to `service: "order-service"`.

> "Services emit structured JSON to Logstash over TCP. Logstash writes to Elasticsearch, Kibana
> reads it. Because the logs are structured, filtering by service is a query, not a grep."

```bash
curl -s "localhost:9200/shopsphere-logs-*/_search?size=0" \
  -H 'Content-Type: application/json' \
  -d '{"aggs":{"per_service":{"terms":{"field":"service.keyword"}}}}'
```

> "One line of curl, all eight services, document counts. This is the same pipeline that would feed
> an APM or alerting rule in production."

---

## Scene 6 — Security (5:10–6:00)

Show the gateway filter, `backend/api-gateway/.../JwtGatewayFilter.java`.

> "The gateway is the only place identity is established. It verifies the JWT, enforces role rules,
> and then **overwrites** the `X-User-Id` and `X-User-Role` headers rather than trusting what the
> caller sent."

Demonstrate, not just claim:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"someone@example.com","password":"Passw0rd!"}' | jq -r .accessToken)

# claim to be an admin
curl -s -o /dev/null -w '%{http_code}\n' -X POST localhost:8080/api/products \
  -H "Authorization: Bearer $TOKEN" -H 'X-User-Id: 1' -H 'X-User-Role: ADMIN' \
  -H 'Content-Type: application/json' -d '{"name":"x","description":"x","price":1,"category":"Tech","sku":"X1","active":true}'
```

`403`, because the role comes from the verified token, never from the header.

---

## Scene 7 — The admin console (6:00–7:00)

Log in as the admin, go to `/admin`.

> "Different navigation, different information density. Metrics, a revenue chart, low-stock alerts,
> and CRUD over the catalogue and stock."

Click **Inventory** — the low-stock column and reserved quantities are live from MongoDB.

**One honest caveat to say if asked:** the admin orders view calls the same `/api/orders` endpoint
as a customer, so it lists the admin's own orders rather than every customer's. There is no
"list all orders" endpoint yet. Knowing your own gaps is a strength.

---

## Scene 8 — Engineering, not just runtime (7:00–7:40)

Switch to the repository, not the browser.

> "Two things I'd expect to be asked about."

**1. CI.** Show `.github/workflows/ci.yml` — a matrix over nine modules plus a job that builds the
Docker image and asserts the jar contains its application classes.

> "That last assertion exists because we shipped a jar once with an empty `BOOT-INF/classes`. It
> built green and failed at runtime with a `ClassNotFoundException`. The build now refuses to
> produce it."

**2. Tests.** Run them on camera; it takes under a minute:

```bash
mvn -B -f backend/order-service/pom.xml test      # 17 tests
mvn -B -f backend/payment-service/pom.xml verify  # 12 tests + a Testcontainers IT
```

> "The payment integration test runs against a real PostgreSQL container, not H2, because the
> guarantee being tested — one charge per order — is a unique-constraint behaviour."

---

## Close (7:40–8:00)

> "Eight services, six Kafka topics, two datastores, an event-driven order flow with compensation,
> idempotent consumers, retry and dead-letter topics, JWT with role enforcement, and a CI pipeline
> that builds the image. The README documents the limits honestly, including the outbox and DLT
> replay work I would do next."

Point at the Eureka screenshot and the Kibana Discover view. End on the storefront.

---

## If something breaks on camera

| Symptom | Cause | Fix |
| --- | --- | --- |
| Gateway 500, `UnknownHostException` naming a container ID | stale Eureka entry after a recreate | `docker compose restart api-gateway` |
| Gateway 401 after changing `JWT_SECRET` | gateway and auth-service disagree on the signing key | recreate both together |
| `kafka-init` exits 2, `set: pipefail: invalid option name` | CRLF shell script | fixed by `.gitattributes`; if it returns, `git config core.autocrlf input` |
| All services refuse to connect to Postgres | `POSTGRES_PASSWORD` in `.env` differs from what the volume was initialised with | match `.env` to the existing database |
| Frontend shows the offline catalogue | gateway down | `docker compose ps` |

## Recording notes

- 1440×1000, browser at 100% zoom, hide bookmarks bar.
- Do not narrate over the loading spinners; pause on the finished frame.
- If asked "is this production ready?" — no, and say so first. Then list the four gaps: outbox,
  DLT replay, Flyway migrations, distributed tracing. That answer usually ends the interview well.
