# Hosting the demo

This is the deployment path that produces a **live URL you can put in a portfolio**. It is separate
from the architecture in `docs/architecture.md`, which stays the source of truth for the real
system. Read [Why one service](#why-one-service) before you skip anything — the trade-off is
deliberate and worth being able to explain in an interview.

## What gets deployed

| Piece | Host | Plan | Cost |
| --- | --- | --- | --- |
| Vue storefront | Netlify | Free | $0 |
| Consolidated API (`backend/demo-service`) | Render | Free | $0 |
| PostgreSQL | Neon | Free | $0 |
| MongoDB | MongoDB Atlas | M0 (shared) | $0 |
| Kafka *(optional)* | Redpanda Cloud | Free | $0 |

Total: **$0**. All five free tiers are permanent, unlike Render's own free Postgres which expires
after 30 days.

## Why one service

The repository runs eight services with Eureka discovery and six Kafka topics. That topology cannot
be hosted on free tiers, for three concrete reasons:

1. **Free web services sleep.** Render parks an idle free instance after ~15 minutes and the first
   request has to wait for a cold start. With eight services the gateway wakes before Eureka has
   registered anything, so a visitor arriving cold sees a 500 instead of a storefront.
2. **No persistent disks.** Kafka cannot run on Render's free tier at all, and Postgres or Mongo
   volumes are wiped on every restart.
3. **512 MB per instance.** Eight Spring Boot JVMs does not fit the budget.

`backend/demo-service` resolves this **without duplicating business logic**. It compiles the six
service source trees directly (via `build-helper-maven-plugin`) and supplies only what the
topology used to provide:

| Real topology | Hosted demo | Why |
| --- | --- | --- |
| `api-gateway` (reactive JWT routing) | `DemoIdentityFilter` (servlet) | Gateway is WebFlux; it cannot share a servlet context |
| Eureka discovery | `SimpleDiscoveryClient` | One process needs no registry |
| Database per service | One database | Neon free allows one database per project |
| `@KafkaListener` consumers | Disabled by default | No broker required to browse; see [Kafka](#enabling-kafka) |

Every entity, DTO, repository, validation rule and security rule is the real service code. What you
are not getting in the hosted build is the *deployment* topology, and the README says so.

## Steps

### 1. Database (Neon)

1. Create a project at [neon.tech](https://neon.tech) on the free plan.
2. Copy the **pooled** connection string. Append `?sslmode=require` if it is not already there.
   Keep the user and password.

### 2. MongoDB (Atlas)

1. Create a free M0 cluster at [cloud.mongodb.com](https://cloud.mongodb.com).
2. Under **Network Access**, allow access from `0.0.0.0/0` (Render has no fixed egress IP).
3. Create a database user and copy the SRV connection string.

### 3. API on Render

1. Push your changes, then in Render choose **New → Blueprint**.
2. Select this repository. Render reads `render.yaml`.
3. When prompted, fill in the variables marked `sync: false`:

   | Variable | Value |
   | --- | --- |
   | `DB_URL` | Neon pooled JDBC URL with `?sslmode=require` |
   | `DB_USERNAME` / `DB_PASSWORD` | Neon credentials |
   | `MONGODB_URI` | Atlas SRV string |
   | `JWT_SECRET` | `openssl rand -base64 48` |
   | `SHOPSPHERE_ADMIN_EMAIL` | admin email for `/admin` |
   | `SHOPSPHERE_ADMIN_PASSWORD` | at least 12 characters |
   | `CORS_ALLOWED_ORIGINS` | your Netlify origin, added in step 4 |

4. Note the API URL, e.g. `https://shopsphere-demo-api.onrender.com`.

`DDL_AUTO=update` creates the schema on first boot and `SEED_MODE=always` seeds the catalogue on
every boot; both inserts are idempotent, so restarts are safe. The admin user is created only if it
does not already exist.

### 4. Storefront on Netlify

1. In Netlify choose **Add new site → Import an existing project** and select this repository.
2. Set the build settings. `netlify.toml` supplies the rest:

   | Setting | Value |
   | --- | --- |
   | Base directory | `frontend/shopsphere-web` |
   | Build command | `npm ci && npm run build` |
   | Publish directory | `dist` |
   | Node version | 20 |

3. Add `VITE_API_URL=https://<your-api>.onrender.com` under **Site configuration → Environment
   variables**. Vite inlines this at build time, so a change requires a redeploy.
4. Copy the resulting `https://<site>.netlify.app` origin into the Render variable
   `CORS_ALLOWED_ORIGINS` and redeploy the API. Browsers block the preflight otherwise.

### 5. Verify

```bash
curl https://<your-api>.onrender.com/api/products?size=2      # public catalogue
curl -i https://<your-api>.onrender.com/api/orders            # expect 401
open https://<your-site>.netlify.app
```

Register a customer in the UI, add to cart and check out. Sign in with the admin credentials at
`/admin`.

## Enabling Kafka

Browsing, auth, cart and the admin console all work without a broker. The **asynchronous order flow**
— `order.created` → `inventory.reserved` → `payment.completed`, which is what moves an order from
PENDING to PAID — needs one.

1. Create a free cluster at [redpanda.com](https://redpanda.com) and get the broker list, e.g.
   `redpanda-xxxx.aws.tppr.redpandadata.com:9092`, plus SASL user and password.
2. Add to Render and redeploy:

   ```
   KAFKA_BOOTSTRAP_SERVERS=redpanda-xxx:9092
   KAFKA_ENABLED=true
   ```

3. Create the six topics (`order.created`, `inventory.reserved`, `inventory.insufficient`,
   `payment.completed`, `payment.failed`, `order.shipped`). Rediscovering them happens through the
   same script the local stack uses:

   ```bash
   docker run --rm -e KAFKA_BOOTSTRAP=<brokers> -v "$PWD/infrastructure/kafka:/scripts:ro" \
     apache/kafka:3.9.1 /bin/bash /scripts/create-topics.sh
   ```

Redpanda speaks the Kafka protocol, so no application code changes. If you would rather model
authentication, prefix the bootstrap value with `SASL_SSL://` and add a
`spring.kafka.properties.sasl.*` setting.

## Costs and limits to state honestly

- **Cold starts.** A free Render instance sleeps when idle. The first request after ~15 minutes can
  take up to a minute. That is the single biggest difference from a paid host.
- **Shared resources.** Neon and Atlas free tiers are shared and rate-limited. This is a demo, not a
  product.
- **No Kafka by default.** Without it, an order stays PENDING, because payment is triggered by
  consuming `inventory.reserved`.
- **One database.** The repository's database-per-service design is real and runs locally with
  Compose; the hosted demo collapses it because Neon free allows one database.

## Local verification

You can run the same artifact locally before deploying anything:

```bash
docker build -f backend/demo.Dockerfile -t shopsphere-demo:local backend
docker run -d -p 8091:8080 \
  -e DB_URL=jdbc:postgresql://localhost:5432/shopsphere_demo \
  -e DB_USERNAME=shopsphere -e DB_PASSWORD=shopsphere_dev \
  -e MONGODB_URI=mongodb://localhost:27017/shopsphere_demo \
  -e CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  shopsphere-demo:local
curl localhost:8091/actuator/health
```

To point the Vue dev server at it instead of the Compose gateway:

```bash
cd frontend/shopsphere-web
VITE_API_URL=http://localhost:8091 npm run dev
```