# eureka-server

Service registry for **Project 2 — Order Platform with AI Risk Scoring**.

| | |
|---|---|
| Port | `8761` |
| Package root | `in.codekerdos.eureka` |
| Eureka app name | n/a (this *is* the registry) |
| Depends on | nothing |

## What it's for

Every business service (`order-service`, `inventory-service`, `payment-service`,
`notification-service`, `ai-risk-service`) and the `api-gateway` register themselves here on
startup and pull each other's live instance list from here. That lets the gateway route requests
with `lb://order-service` (a logical, load-balanced Eureka application name) instead of a
hardcoded `host:port`, and lets services come up, restart, or scale without anyone needing to edit
config — the registry is always the current source of truth for "who is alive and where."

This module runs **standalone**: it does not register itself and does not fetch a registry from
anywhere else (`eureka.client.register-with-eureka: false`, `eureka.client.fetch-registry: false`
in `application.yml`), since it is the only registry instance in this project.

## Run it

```bash
./mvnw spring-boot:run
```

Dashboard: **http://localhost:8761**

Once other Project 2 services start (each configured with
`eureka.client.service-url.defaultZone: http://localhost:8761/eureka`), they will appear in the
"Instances currently registered with Eureka" table on that dashboard.

Start this service **first** — the gateway and every business service depend on it being up to
register/discover each other (they retry on failure, but nothing routes correctly until this is
running).

## Docker

Built as part of the Project 2 stack — see `api-gateway/README.md` for the full
`docker-compose.yml` (which lives in that folder) covering the whole platform, this service
included.
