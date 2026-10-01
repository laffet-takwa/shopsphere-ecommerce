# Deployment

## Docker Compose

Copy `.env.example` to `.env`, set a strong random JWT secret and local database password, then run `docker compose up --build`. Compose uses one-node Kafka in KRaft mode, one PostgreSQL instance with four separate databases, separate MongoDB databases, and an unsecured single-node Elasticsearch for local demonstration. `docker-compose.dev.yml` additionally publishes service ports for debugging.

A one-shot `kafka-init` container creates the six event topics with three partitions before any service starts, so partition counts are a deliberate choice rather than a broker default. It is safe to re-run. MongoDB indexes are created by Spring Data from the `@Indexed` annotations on the document classes, since `auto-index-creation` is enabled; `infrastructure/mongodb/README.md` explains why no `init.js` hook is shipped.

Useful checks: Eureka dashboard `http://localhost:8761`, gateway health `http://localhost:8080/actuator/health`, Kibana `http://localhost:5601`, and Elasticsearch `http://localhost:9200`. Search the `shopsphere-logs-*` index in Kibana after creating requests.

### Rebuilding a single service

`docker compose up -d --force-recreate <service>` gives the container a new IP, but the gateway keeps the address it learned from Eureka until that registration expires. Requests in that window fail with a gateway 500 and a `UnknownHostException` naming the dead container, even though the service itself is healthy. Restart the gateway to force re-resolution:

```bash
docker compose up -d --force-recreate product-service
docker compose restart api-gateway
```

The same applies to any deploy that replaces containers behind discovery. Confirm the new instance in the Eureka dashboard before treating a gateway 500 as an application fault.

## Kubernetes

Manifests target a local cluster with an Ingress controller installed. Apply namespace, secret/config, infrastructure, application, then ingress manifests:

```bash
kubectl apply -f kubernetes/namespace.yaml
kubectl apply -f kubernetes/secrets/shopsphere-secret.yaml
kubectl apply -f kubernetes/configmaps/shopsphere-config.yaml
kubectl apply -f kubernetes/infrastructure/
kubectl apply -f kubernetes/services/
kubectl apply -f kubernetes/ingress.yaml
```

The `kafka-create-topics` Job in `kubernetes/infrastructure/kafka.yaml` provisions the event topics once. Jobs are immutable, so if you need a different partition count, delete the job (`kubectl delete job kafka-create-topics -n shopsphere`) and re-apply rather than editing it in place.

Build/tag/push each independently deployable image to the registry referenced in `kubernetes/services/apps.yaml` before applying. Replace every example secret, image, storage class, ingress host, and resource setting for the target cluster. The manifests use single replicas and development-scale storage; they are not an HA/security baseline.

## Production Checklist

- Move from database-per-schema on one PostgreSQL host to independently managed instances/roles and automated migrations.
- Enable TLS, authenticated Kafka, Elasticsearch security, network policies, pod security, and non-root containers.
- Use a secret manager; rotate the JWT key and implement key IDs/rotation.
- Add transactional outbox, event contract compatibility, DLT operations, tracing, metrics, and alerting.
- Add resource requests/limits, autoscaling, backups/restore tests, and multi-zone persistent storage.