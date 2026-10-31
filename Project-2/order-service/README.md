# order-service

> Project #2 — Order Platform with AI Risk Scoring. Port **8081**, Eureka app name `order-service`.

## Role

`order-service` is two things at once in Project 2:

1. **The auth source for the whole system.** It owns the `app_users` table and is the *only*
   service that issues JWTs (`POST /api/auth/login`). Every other business service
   (inventory, payment, notification, ai-risk) and the gateway only *validate* tokens signed
   with the shared `JWT_SECRET` — they never mint one.
2. **The saga orchestrator for the order lifecycle.** It owns the `orders` table and the state
   machine that decides whether an order is ultimately `APPROVED`, `HELD`, or `REJECTED` based
   on three independent signals that arrive asynchronously over Kafka: inventory reservation,
   payment, and AI risk scoring.

## Auth endpoints

| Method | Path | Who |
|---|---|---|
| POST | `/api/auth/register` | anyone — always creates a `CUSTOMER` |
| POST | `/api/auth/login` | anyone — returns a JWT |

```bash
# Register
curl -s -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@codekerdos.in","password":"secret123","fullName":"Jane Doe"}'

# Login
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"customer@codekerdos.in","password":"cust123"}' | jq -r .token)
```

### Seeded users (H2 profile, `DataLoader`)

| Email | Password | Role |
|---|---|---|
| customer@codekerdos.in | cust123 | CUSTOMER |
| admin@codekerdos.in | admin123 | ADMIN |

## Order endpoints

| Method | Path | Who |
|---|---|---|
| POST | `/api/orders` | CUSTOMER |
| GET | `/api/orders/{id}` | the owning CUSTOMER or an ADMIN |
| GET | `/api/orders/mine` | CUSTOMER |
| GET | `/api/orders` | ADMIN |

```bash
# Place an order
curl -s -X POST http://localhost:8081/api/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"sku":"SKU-123","quantity":2,"totalAmount":149.99}'

# Check status later
curl -s http://localhost:8081/api/orders/{id} -H "Authorization: Bearer $TOKEN"

# My orders
curl -s http://localhost:8081/api/orders/mine -H "Authorization: Bearer $TOKEN"
```

## The order saga

```
POST /api/orders
      │
      ▼
Order saved: status=PENDING_RISK, inventoryStatus=PENDING, paymentStatus=PENDING
      │
      ▼
Kafka "order-events" ── ORDER_CREATED {sku, quantity, totalAmount, customerEmail} ──▶
                                                          │              │            │
                                              inventory-service   payment-service  ai-risk-service
                                                          │              │            │
                                              INVENTORY_RESERVED   PAYMENT_COMPLETED  RISK_SCORED
                                              or INVENTORY_REJECTED  or PAYMENT_FAILED  {decision, riskScore, reasoning}
                                                          │              │            │
                                                          └──────────────┴────────────┘
                                                                         ▼
                                                          order-service's own OrderEventListener
                                                          (group "order-service") updates the
                                                          matching signal field and re-evaluates
```

**Publishes** to `order-events`: `ORDER_CREATED` on create, then exactly one of
`ORDER_APPROVED` / `ORDER_HELD` / `ORDER_REJECTED` once the saga concludes (consumed by
`notification-service`).

**Consumes** from `order-events` (own consumer group `order-service`, so it also receives its own
outbound events — those are ignored in the listener's `switch`): `INVENTORY_RESERVED`,
`INVENTORY_REJECTED`, `PAYMENT_COMPLETED`, `PAYMENT_FAILED`, `RISK_SCORED`.

### Evaluation rule (`OrderService#evaluateOrder`)

Runs after every signal update. If the order is still `PENDING_RISK` **and** all three signals
have reported (`inventoryStatus` and `paymentStatus` are no longer `PENDING`, and `riskDecision`
is no longer `null`):

- `inventoryStatus == REJECTED || paymentStatus == REJECTED` → **REJECTED**
- else `riskDecision == "REJECT"` → **REJECTED**
- else `riskDecision == "HOLD"` → **HELD**
- else → **APPROVED**

The matching terminal event is published. If not all three signals are in yet, the partial state
is saved and the order simply **stays `PENDING_RISK`** — this is what "queues instead of failing"
means: if `ai-risk-service` (or its Groq call) is down/slow, the order just waits for `RISK_SCORED`
instead of erroring out. Only if `ai-risk-service` itself is not running at all does the order stay
`PENDING_RISK` indefinitely (the Week 5 chaos-test case).

### Redis idempotency — database index 0

Every `@KafkaListener` method here starts with `idempotencyService.markProcessedIfNew(event.eventId())`
before acting, using key pattern `processed:<eventId>`. All 5 business services share one Redis
container, but each uses its **own logical database index** (`spring.data.redis.database`) so the
same `eventId` being processed independently by, say, inventory-service and payment-service never
collides on the same key. `order-service` uses **database `0`**.

## Run standalone (H2, no Docker)

```bash
cp .env.example .env
# set JWT_SECRET if you want to override the demo default

./mvnw spring-boot:run
```

- API: http://localhost:8081
- Swagger: http://localhost:8081/swagger-ui.html
- Health: http://localhost:8081/actuator/health
- H2 console: http://localhost:8081/h2-console

Kafka/Redis/Eureka calls just fail quietly/retry in the background with no broker reachable — the
REST auth + order-create API still works, but the saga will never progress past `PENDING_RISK`
since no downstream service consumes `ORDER_CREATED`.

## Run via docker compose

`docker-compose.yml` lives in `api-gateway/` and builds this service via
`build: ../order-service`, alongside Eureka, the gateway, inventory/payment/notification/
ai-risk services, Kafka (KRaft), Redis, Postgres, and Zipkin.

```bash
cd ../api-gateway
docker compose up --build
```

Postgres profile note: Project 2 uses **one shared** dev database `order_platform_db` for all 5
business services (simpler than Project 3's database-per-service) — `order-service`'s tables are
`app_users` and `orders`, distinct table names avoid collisions with the other services sharing
the same database.
