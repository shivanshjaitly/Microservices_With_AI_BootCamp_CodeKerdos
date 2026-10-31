# Project 2 — Order Platform with AI Risk Scoring

CodeKerdos Microservices Bootcamp · Weeks 3–5 · **Java 17 · Spring Boot 3.4.1 · Spring Cloud 2024.0.0**

> **Do not use Spring Boot 4.x** with Eureka/Gateway — use Boot **3.4.1** and Cloud **2024.0.0** or startup fails with `EurekaAutoServiceRegistration` / `ServerProperties` errors.

## Modules

| Folder | Port | Eureka name | Role |
|--------|------|-------------|------|
| [`eureka-server`](eureka-server/) | 8761 | (registry) | Service discovery phone book |
| [`api-gateway`](api-gateway/) | 8080 | `api-gateway` | Single front door + JWT edge check |
| [`order-service`](order-service/) | 8081 | `order-service` | **JWT issuer**, orders, saga orchestrator |
| [`inventory-service`](inventory-service/) | 8082 | `inventory-service` | Stock reservation (Kafka) |
| [`payment-service`](payment-service/) | 8083 | `payment-service` | Payment (Kafka) |
| [`notification-service`](notification-service/) | 8084 | `notification-service` | Customer notify on terminal orders |
| [`ai-risk-service`](ai-risk-service/) | 8085 | `ai-risk-service` | Groq risk scoring (Kafka) |

## Start order (local)

1. **Eureka** → http://localhost:8761  
2. All **five business services** (8081–8085) — each needs `spring.application.name` matching the table above and `eureka.client` in `application.yml`.  
3. **API Gateway** → http://localhost:8080  

With `spring-cloud-starter-netflix-eureka-client` on the classpath, services **auto-register** even if you omit an `eureka:` block — but you should set **`spring.application.name`** explicitly (wrong names break `lb://` routes).

## Auth & JWT (Week 3 Sunday)

- **Only `order-service` mints JWTs** (`POST /api/auth/login`, `/api/auth/register`).  
- **Gateway + all business services validate** the same `JWT_SECRET` (`app.jwt.secret` / env).  
- **Login/register:** POST + JSON body only — **no** `Authorization: Bearer` on those calls.  
- **Other APIs:** `Authorization: Bearer <token>` (header name must be **`Authorization`**, not a custom name).

### Postman — register & login

**Register** — `POST http://localhost:8081/api/auth/register` (or `:8080` via gateway)

```json
{
  "email": "jane@codekerdos.in",
  "password": "secret123",
  "fullName": "Jane Doe"
}
```

**Login** — `POST http://localhost:8081/api/auth/login`

```json
{
  "email": "jane@codekerdos.in",
  "password": "secret123"
}
```

Copy `token` from the response. On later requests: **Authorization → Bearer Token** → paste `eyJ...`.

### How JWT fits together

| Piece | Job |
|-------|-----|
| `JwtService` | Sign token at login; verify + read claims on later requests |
| `JwtAuthFilter` | On each request: if `Authorization: Bearer` present → validate → set Spring Security user; if no header → pass through |
| `SecurityConfig` | Public paths (`/api/auth/**`); password check via `AuthenticationManager` on login; stateless sessions |

First visit: filter skips JWT (no Bearer). Login uses **email/password**, not Bearer.

## Order saga & Kafka (Weeks 4–5)

- **`OrderService`** saves `PENDING_RISK`, publishes **`ORDER_CREATED`**, updates inventory/payment/risk signals from Kafka, runs **`evaluateOrder`** → `APPROVED` / `HELD` / `REJECTED`.  
- **`OrderEventProducer`** uses **`KafkaTemplate`** to send to topic **`order-events`** (key = `orderId`).  
- See [`order-service/README.md`](order-service/README.md) for curl examples and evaluation rules.

## Docker (full stack)

```bash
cd api-gateway
docker compose up --build
```

Compose file lives in **`api-gateway/`** and builds sibling modules (`../order-service`, etc.).

## Per-service docs

- [eureka-server/README.md](eureka-server/README.md)  
- [api-gateway/README.md](api-gateway/README.md)  
- [order-service/README.md](order-service/README.md)  
- [inventory-service/README.md](inventory-service/README.md)  
- [payment-service/README.md](payment-service/README.md)  
- [notification-service/README.md](notification-service/README.md)  
- [ai-risk-service/README.md](ai-risk-service/README.md)  

## Build all modules

From this directory:

```bash
mvn -q -pl order-service -am test compile
```

Or run each service with `./mvnw spring-boot:run` inside its folder.
