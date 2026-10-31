# Inventory Service (`inventory-service`)

Part of **Project 2 — Order Platform with AI Risk Scoring**. Port **8082**, Eureka app name
`inventory-service`, package root `in.codekerdos.inventory`.

## Role

Tracks available stock per SKU. Reacts to the `order-events` Kafka topic:

- Consumes `ORDER_CREATED` (payload: `sku`, `quantity`, `totalAmount`, `customerEmail`).
- Attempts to reserve `quantity` units of `sku` using an atomic conditional `UPDATE`
  (`StockItemRepository.decrementIfAvailable`) — the availability check and the decrement happen in
  one SQL statement, so concurrent reservations for the same SKU can never oversell it.
- Publishes **`INVENTORY_RESERVED`** (payload: `sku`, `quantity`) if there was enough stock, or
  **`INVENTORY_REJECTED`** (payload: `sku`, `reason`) if the SKU is unknown or short on stock.
- Ignores every other event type on the topic (it is one of several independent consumer groups
  reading the same topic; `notification-service`, `payment-service`, `ai-risk-service` and
  `order-service` each see every event too and filter for what they care about).

## REST API

- `GET /api/inventory/{sku}` — current stock level for a SKU (any authenticated user).
- `POST /api/inventory/restock` — `{ "sku": "...", "quantity": 10 }`, adds to stock. Requires
  `ROLE_ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`). Creates the SKU if it doesn't exist yet.

Swagger UI: `http://localhost:8082/swagger-ui.html`.

## Seeded data

`DataLoader` seeds three SKUs on startup:

| SKU | Quantity |
|---|---|
| `SKU-LAPTOP-001` | 50 |
| `SKU-PHONE-002` | 100 |
| `SKU-HEADSET-003` | 5 (deliberately low — order more than 5 headsets in class to see `INVENTORY_REJECTED` fire) |

## Redis idempotency

Every Kafka listener checks `IdempotencyService.markProcessedIfNew(event.eventId())` before acting,
using the key pattern `processed:<eventId>` (TTL 24h). Inventory-service uses Redis **logical
database index 1** (`spring.data.redis.database: 1`) so its idempotency keys never collide with the
other services sharing the same Redis container — each service (order=0, inventory=1, payment=2,
notification=3, ai-risk=4) gets its own logical database rather than namespacing the key itself.

## Persistence

Multi-profile `application.yml`:

- `h2` (default, for local/IDE runs): in-memory `inventorydb`, H2 console at `/h2-console`.
- `postgres` (used in Docker Compose): connects to the **shared** `order_platform_db` Postgres
  database used by all 5 Project 2 business services. Table collisions are avoided because each
  service uses its own table name — inventory-service owns `stock_items`.

## Run standalone

```bash
cd inventory-service
cp .env.example .env   # edit if needed
./mvnw spring-boot:run
```

Needs a Kafka broker on `localhost:29092` and Redis on `localhost:6379` reachable (or the consumer/
Redis calls will fail/retry) — Eureka registration will also retry harmlessly if `localhost:8761`
isn't up yet.

## Run via Docker Compose

Built and orchestrated from `api-gateway/docker-compose.yml` as service `inventory-service`,
using the `postgres` Spring profile, `kafka:9092`, `redis`, and `eureka-server` hostnames from the
compose network.
