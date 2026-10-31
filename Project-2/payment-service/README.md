# Payment Service (`payment-service`)

Part of **Project 2 — Order Platform with AI Risk Scoring**. Port **8083**, Eureka app name
`payment-service`, package root `in.codekerdos.payment`.

## Role

Simulates a payment gateway charge for each order. Reacts to the `order-events` Kafka topic:

- Consumes `ORDER_CREATED` (payload: `sku`, `quantity`, `totalAmount`, `customerEmail`).
- "Charges" `totalAmount` via `PaymentService.charge(...)` and records a `Payment` row.
- Publishes **`PAYMENT_COMPLETED`** (payload: `orderId`, `amountCharged`) or **`PAYMENT_FAILED`**
  (payload: `orderId`, `reason`).
- Ignores every other event type on the topic.

## Payment-failure simulation rule (demoable in class)

`PaymentService.charge(...)` decides pass/fail with two rules, checked in order:

1. **Deterministic**: if `totalAmount` exceeds `app.payment.max-auto-approve-amount`
   (default **5000**, env override `MAX_AUTO_APPROVE_AMOUNT`), the charge is **always declined** —
   reason: `"Amount <amount> exceeds max auto-approve threshold of <threshold>"`. Place an order for
   an item over the threshold in class to reliably trigger `PAYMENT_FAILED`.
2. **Random**: otherwise, there's a small random decline chance —
   `app.payment.random-failure-rate` (default **0.10** = 10%, env override `RANDOM_FAILURE_RATE`) —
   to simulate a flaky real-world payment gateway. Reason:
   `"Simulated payment gateway decline (random failure rate <rate>)"`.

Both rules are evaluated in `PaymentService.decide(BigDecimal)` (see
`src/main/java/in/codekerdos/payment/service/PaymentService.java`).

## REST API

- `GET /api/payments/{orderId}` — the payment record for an order (any authenticated user).

Swagger UI: `http://localhost:8083/swagger-ui.html`.

## Redis idempotency

Every Kafka listener checks `IdempotencyService.markProcessedIfNew(event.eventId())` before acting,
using the key pattern `processed:<eventId>` (TTL 24h). Payment-service uses Redis **logical database
index 2** (`spring.data.redis.database: 2`) so its idempotency keys never collide with the other
services sharing the same Redis container — each service (order=0, inventory=1, payment=2,
notification=3, ai-risk=4) gets its own logical database rather than namespacing the key itself.

## Persistence

Multi-profile `application.yml`:

- `h2` (default, for local/IDE runs): in-memory `paymentdb`, H2 console at `/h2-console`.
- `postgres` (used in Docker Compose): connects to the **shared** `order_platform_db` Postgres
  database used by all 5 Project 2 business services. Table collisions are avoided because each
  service uses its own table name — payment-service owns `payments`.

## Run standalone

```bash
cd payment-service
cp .env.example .env   # edit if needed
./mvnw spring-boot:run
```

Needs a Kafka broker on `localhost:29092` and Redis on `localhost:6379` reachable (or the consumer/
Redis calls will fail/retry) — Eureka registration will also retry harmlessly if `localhost:8761`
isn't up yet.

## Run via Docker Compose

Built and orchestrated from `api-gateway/docker-compose.yml` as service `payment-service`,
using the `postgres` Spring profile, `kafka:9092`, `redis`, and `eureka-server` hostnames from the
compose network.
