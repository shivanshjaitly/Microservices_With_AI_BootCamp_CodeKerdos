# AI Risk Service (ai-risk-service)

Part of **Project 2 — Order Platform with AI Risk Scoring**.

- Port: **8085**
- Package root: `in.codekerdos.airisk`
- Eureka app name: `ai-risk-service`

## Role

Consumes `ORDER_CREATED` off the shared `order-events` Kafka topic (consumer
group `ai-risk-service`), calls **Groq** (via Spring AI's `ChatClient`,
Groq's OpenAI-compatible endpoint) for a forced-structured-JSON risk decision:

```json
{"decision": "APPROVE|HOLD|REJECT", "riskScore": 0-100, "reasoning": "..."}
```

and publishes `RISK_SCORED` back onto `order-events` with that payload for
order-service to fold into the order's saga state. Every decision — happy
path AND fallback — is persisted to the `risk_decisions` table and logged at
INFO level, so there is always an audit trail explaining *why* an order got
its risk decision.

All other event types on the topic (`INVENTORY_*`, `PAYMENT_*`,
`ORDER_APPROVED/HELD/REJECTED`) are ignored — this service only cares about
`ORDER_CREATED`.

## Resilience — CircuitBreaker + Retry + Bulkhead around the Groq call

`AiRiskService.assessRisk(...)` is wrapped with Resilience4j's
`@CircuitBreaker`, `@Retry`, and `@Bulkhead`, all under one named instance
**`groqRiskCall`** (config in `application.yml`):

| Concern | Config |
|---|---|
| Circuit breaker | sliding window 10 calls, opens at ≥50% failure rate (min 5 calls sampled), stays open 20s, 3 trial calls in half-open |
| Retry | up to 3 attempts, 500ms between attempts |
| Bulkhead | max 5 concurrent Groq calls |

**What happens when Groq is flaky / the circuit is open:**
`fallbackRiskDecision(event, throwable)` returns a safe default —
`RiskDecision("HOLD", 50, "AI risk engine unavailable — defaulted to HOLD for
manual review (circuit breaker open / Groq call failing)")` — which is still
persisted, logged, AND published as a `RISK_SCORED` event. Orders are never
silently dropped just because Groq is misbehaving; they simply queue for
manual review instead.

**What happens if the WHOLE ai-risk-service process is killed** (the Week 5
chaos-test scenario): nothing in this service runs at all — there is no
Kafka consumer to react to `ORDER_CREATED`, so no `RISK_SCORED` event is ever
published for orders created while this service is down. That is expected
and correct: order-service's saga simply leaves those orders in
`PENDING_RISK` until this service comes back and (re-)processes the backlog
from its Kafka consumer group offset. Fixing that is order-service's
responsibility (queuing instead of failing), not this service's.

## Groq / Spring AI setup

Get a free Groq API key at **[console.groq.com](https://console.groq.com)**
(sign up, create an API key), then set it in your `.env`:

```bash
cp .env.example .env
# edit .env and set GROQ_API_KEY=gsk_...
```

Model used: `llama-3.3-70b-versatile` via Groq's OpenAI-compatible endpoint
(`https://api.groq.com/openai`). If `GROQ_API_KEY` is unset, real calls to
Groq will fail — the circuit breaker will open after enough failures and
every order will get the safe-default HOLD fallback (still fully functional
for a demo, just not "real" AI risk scoring).

## Redis idempotency — database index 4

Every Kafka consumer in Project 2 shares one Redis instance but uses its own
**logical database index** so that the same `eventId` being processed by
multiple independent services never collides on the same key
(`processed:<eventId>`). This service uses **database 4**
(`spring.data.redis.database: 4`). See `docs/CONVENTIONS.md` / the Project 2
shared contract for the full table of indexes across services.

## REST API

- `GET /api/risk/{orderId}` — the most recent logged risk decision for that
  order (authenticated, 404 via `ResourceNotFoundException` if none yet).

All endpoints require a `Bearer` JWT issued by order-service's `/api/auth/login`
(shared `JWT_SECRET` across all Project 2 services — this service validates
the token's claims directly, it does not own a user table).

Swagger UI: `http://localhost:8085/swagger-ui.html`

## Run standalone (H2, no Docker)

```bash
cd ai-risk-service
cp .env.example .env   # set GROQ_API_KEY and JWT_SECRET
./mvnw spring-boot:run
```

Requires Kafka and Redis reachable at `localhost:29092` / `localhost:6379`
(bring them up via `docker compose up kafka redis` from `api-gateway/`),
and (optionally) Eureka Server running at `localhost:8761` for service discovery.

## Run via docker compose

This service is built as part of the full Project 2 stack — see
`docker-compose.yml` in `api-gateway/`, which builds this service via
`build: ../ai-risk-service` and wires up Kafka, Redis, Postgres,
Eureka, and Zipkin. In compose the `postgres` Spring profile is activated and
all 5 business services share one Postgres database `order_platform_db`
(this service's rows live in its own `risk_decisions` table, so there's no
collision with other services' tables). Remember to set `GROQ_API_KEY` in the
compose `.env` file for real Groq calls to work.

## Tests

`AiRiskServiceTest` (Mockito) mocks the fluent `ChatClient` chain
(`ChatClient.ChatClientRequestSpec` / `ChatClient.CallResponseSpec`) to verify
a successful Groq call returns the model's decision and persists a log row,
that an invalid `decision` value from the model is clamped to `HOLD`, and
that `fallbackRiskDecision` returns the documented safe default and persists
its own auditable log row.
