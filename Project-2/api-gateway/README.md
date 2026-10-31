# api-gateway

Edge gateway for **Project 2 — Order Platform with AI Risk Scoring**.

| | |
|---|---|
| Port | `8080` |
| Package root | `in.codekerdos.gateway` |
| Eureka app name | `api-gateway` |
| Depends on | `eureka-server` (must be running first) |

## Role in the system

Every client request into Project 2 goes through this gateway first. It is a reactive
(`spring-cloud-starter-gateway`, WebFlux-based) service that does two things:

1. **Routes** requests to the right business service by Eureka application name (`lb://...`), so
   the gateway never needs to know a service's actual host/port — it asks the registry.
2. **Fast-fails unauthenticated requests** with a `401` before they're even proxied downstream, via
   a `GlobalFilter` (`JwtValidationGatewayFilter`) that checks the `Authorization: Bearer <token>`
   header against the same shared `JWT_SECRET` every other Project 2 service uses. This is a
   convenience edge guard, not the only line of defense — every downstream service independently
   validates the token again (see each service's own `JwtAuthFilter`), since services can also be
   called directly (service-to-service, tests) without going through the gateway.

## Route table

Explicit routes are declared in `application.yml` under `spring.cloud.gateway.routes` (not the
discovery locator — explicit routes are clearer to read and to teach). Route id == the target
Eureka application name in every case.

| Path prefix | Route id | Target | Public? |
|---|---|---|---|
| `/api/auth/**` | `order-service` | `lb://order-service` | Yes — login/register must stay public |
| `/api/orders/**` | `order-service` | `lb://order-service` | No |
| `/api/inventory/**` | `inventory-service` | `lb://inventory-service` | No |
| `/api/payments/**` | `payment-service` | `lb://payment-service` | No |
| `/api/notifications/**` | `notification-service` | `lb://notification-service` | No |
| `/api/risk/**` | `ai-risk-service` | `lb://ai-risk-service` | No |

`/api/auth/**` and `/api/orders/**` both target `order-service`, so they're declared as two
`Path=` patterns on one `order-service` route rather than two routes with a duplicate id.

The excluded (public) path list lived in code would mean a redeploy every time it changes, so it's
a config property instead — `app.security.public-paths` — read by `JwtValidationGatewayFilter` at
request time via `AntPathMatcher`:

```
/api/auth/**, /actuator/health, /actuator/info, /v3/api-docs/**, /swagger-ui/**
```

## Service discovery

This service is a Eureka client (`spring-cloud-starter-netflix-eureka-client`), registering itself
as `api-gateway` and fetching the live registry from `eureka-server`
(`eureka.client.service-url.defaultZone`, default `http://localhost:8761/eureka`). It resolves each
`lb://<service-id>` route target against whatever instances of that service are currently
registered — no hardcoded host/port anywhere in this module.

## Run it

Start the registry first, then this service:

```bash
# in eureka-server/
./mvnw spring-boot:run

# in api-gateway/ (separate terminal)
cp .env.example .env   # set JWT_SECRET if you're overriding the demo default
./mvnw spring-boot:run
```

Gateway listens on **http://localhost:8080**. Actuator is exposed at `/actuator/health`,
`/actuator/info`, `/actuator/metrics`, and `/actuator/gateway` (route inspection).

This module has no REST controllers of its own (springdoc/swagger is intentionally skipped here —
it's a pure reactive proxy), so there is no local Swagger UI; each downstream service exposes its
own `/swagger-ui.html`.

## Docker / full stack

`docker-compose.yml` for the **entire Project 2 stack** (eureka-server, api-gateway, order-service,
inventory-service, payment-service, notification-service, ai-risk-service, plus Kafka, Redis,
Postgres, and Zipkin) lives in **this folder** (`api-gateway/docker-compose.yml`), matching
the "compose file lives with the gateway/first service" convention used across this repo. That file
is added in a later step once all Project 2 services exist — this README is just noting where it
will live. Once it exists, bring up the whole platform with:

```bash
docker compose up --build
```
