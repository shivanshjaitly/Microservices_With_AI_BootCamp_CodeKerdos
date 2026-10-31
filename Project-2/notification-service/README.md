# Notification Service (notification-service)

Part of **Project 2 — Order Platform with AI Risk Scoring**.

- Port: **8084**
- Package root: `in.codekerdos.notification`
- Eureka app name: `notification-service`

## Role

Consumes the shared `order-events` Kafka topic in its own consumer group
(`notification-service`) and simulates sending a customer notification for the
**terminal** order outcomes only:

| Event type       | Message sent to customer                                                          |
|-------------------|-------------------------------------------------------------------------------------|
| `ORDER_APPROVED`  | "Your order #\<id> has been approved and is being processed."                       |
| `ORDER_HELD`      | "Your order #\<id> is on hold pending manual review."                               |
| `ORDER_REJECTED`  | "Your order #\<id> was rejected. \<reasoning if present in the event payload>."      |

All other event types on the topic (`ORDER_CREATED`, `INVENTORY_RESERVED`,
`INVENTORY_REJECTED`, `PAYMENT_COMPLETED`, `PAYMENT_FAILED`, `RISK_SCORED`) are
intentionally ignored — they are intermediate saga signals other services react
to, not customer-facing outcomes.

"Sending" a notification, for this teaching demo, means: log the message at
INFO level and persist a `NotificationLog` row (no real email/SMS provider is
wired up).

## Redis idempotency — database index 3

Every Kafka consumer in Project 2 shares one Redis instance but uses its own
**logical database index** so that the same `eventId` being processed by
multiple independent services never collides on the same key
(`processed:<eventId>`). This service uses **database 3**
(`spring.data.redis.database: 3`). See `docs/CONVENTIONS.md` / the Project 2
shared contract for the full table of indexes across services.

## REST API

- `GET /api/notifications` — list all notification log rows (authenticated, no
  pagination — small demo scale).
- `GET /api/notifications/{orderId}` — notifications for one order
  (authenticated, 404 if none found yet).

All endpoints require a `Bearer` JWT issued by order-service's `/api/auth/login`
(shared `JWT_SECRET` across all Project 2 services — this service validates the
token's claims directly, it does not own a user table).

Swagger UI: `http://localhost:8084/swagger-ui.html`

## Run standalone (H2, no Docker)

```bash
cd notification-service
cp .env.example .env   # edit if needed
./mvnw spring-boot:run
```

Requires Kafka and Redis reachable at `localhost:29092` / `localhost:6379`
(bring them up via `docker compose up kafka redis` from `api-gateway/`),
and (optionally) Eureka Server running at `localhost:8761` for service discovery.

## Run via docker compose

This service is built as part of the full Project 2 stack — see
`docker-compose.yml` in `api-gateway/`, which builds this service via
`build: ../notification-service` and wires up Kafka, Redis, Postgres,
Eureka, and Zipkin. In compose the `postgres` Spring profile is activated and
all 5 business services share one Postgres database `order_platform_db`
(this service's rows live in its own `notification_logs` table, so there's no
collision with other services' tables).

## Tests

`NotificationEventListenerTest` (Mockito) verifies the correct message is
built per terminal event type and that duplicate deliveries / non-terminal
event types are skipped without touching the repository.
