# Project 2 — Order Platform: Docker + Postman cheat sheet
Use **only the gateway** for normal testing: `http://localhost:8080` (not 8081–8085).
---
## 1. Prerequisites
- Docker Desktop running  
- Optional: `jq` (for curl examples)  
- **Groq** (AI risk): create `.env` next to `docker-compose.yml`
**File:** `Microservices_BootCamp/week-03-api-gateway/.env`
```env
JWT_SECRET=codekerdos-demo-secret-change-in-prod-min-32-chars
GROQ_API_KEY=your-groq-key-here
All services read the same JWT_SECRET. Never commit real API keys.

2. Start the stack (Docker)

cd Microservices_BootCamp/week-03-api-gateway
docker compose up --build -d
First boot: Java containers can take 5–10 minutes. Check:


docker compose ps
Check	URL / command
Eureka
http://localhost:8761
Gateway health
http://localhost:8080/actuator/health
Zipkin (traces)
http://localhost:9411
If compose stops on “eureka-server unhealthy”
Eureka often works even when Docker marks it unhealthy. Start apps manually:


docker start api-gateway order-service inventory-service \
  payment-service notification-service ai-risk-service
Wait until Eureka shows registered apps, then test through 8080.

Stop everything

docker compose down
Stop and wipe DB volume

docker compose down -v
3. Ports (reference)
Service	Port
API Gateway
8080 ← use this
Order (+ auth)
8081
Inventory
8082
Payment
8083
Notification
8084
AI Risk
8085
Eureka
8761
Postgres
5432
Redis
6379
Kafka (host)
29092
Zipkin
9411
4. Demo credentials (you create them)
Register once, then reuse the same user.

Field	Value
Email
jane@codekerdos.in
Password
secret123
Full name
Jane Doe
H2 / local profile only (not Docker Postgres by default): seeded users may exist — see order-service README (customer@codekerdos.in / admin@codekerdos.in). On Docker, register your own user.

5. Gateway routes (all via baseUrl)
Prefix	Service
/api/auth/**, /api/orders/**
order-service
/api/inventory/**
inventory-service
/api/payments/**
payment-service
/api/notifications/**
notification-service
/api/risk/**
ai-risk-service
Public (no token): /api/auth/**, gateway /actuator/health, /actuator/info, swagger paths on gateway.

Everything under /api/orders, /api/inventory, etc. needs Authorization: Bearer <token>.

6. Postman setup
Collection variables
Variable	Initial value
baseUrl
http://localhost:8080
token
(empty)
orderId
(empty)
Collection auth
Type: Bearer Token
Token: {{token}}
Exception: Register and Login → Authorization = No auth
Login → save token (Tests tab)

const body = pm.response.json();
pm.collectionVariables.set("token", body.token);
Create order → save order id (Tests tab)

const body = pm.response.json();
pm.collectionVariables.set("orderId", body.id);
7. Endpoints (through gateway)
Auth (public)
Name	Method	Path	Body
Register
POST
{{baseUrl}}/api/auth/register
See below
Login
POST
{{baseUrl}}/api/auth/login
See below
Register body


{
  "email": "jane@codekerdos.in",
  "password": "secret123",
  "fullName": "Jane Doe"
}
Login body


{
  "email": "jane@codekerdos.in",
  "password": "secret123"
}
Login response (example)


{
  "token": "<jwt>",
  "email": "jane@codekerdos.in",
  "fullName": "Jane Doe",
  "role": "CUSTOMER"
}
Orders (CUSTOMER token)
Name	Method	Path
Create order
POST
{{baseUrl}}/api/orders
My orders
GET
{{baseUrl}}/api/orders/mine
Get one order
GET
{{baseUrl}}/api/orders/{{orderId}}
All orders
GET
{{baseUrl}}/api/orders
Create order body (happy path SKU)


{
  "sku": "SKU-LAPTOP-001",
  "quantity": 1,
  "totalAmount": 99.99
}
Seeded stock (inventory DataLoader): SKU-LAPTOP-001 (50), SKU-PHONE-002 (100), SKU-HEADSET-003 (5).

Inventory
Name	Method	Path	Who
Stock by SKU
GET
{{baseUrl}}/api/inventory/SKU-LAPTOP-001
Authenticated
Restock
POST
{{baseUrl}}/api/inventory/restock
ADMIN
Restock body


{
  "sku": "SKU-LAPTOP-001",
  "quantity": 10
}
Payment
Name	Method	Path
Payment for order
GET
{{baseUrl}}/api/payments/{{orderId}}
Notifications
Name	Method	Path
All logs
GET
{{baseUrl}}/api/notifications
By order
GET
{{baseUrl}}/api/notifications/{{orderId}}
AI risk
Name	Method	Path
Risk decision
GET
{{baseUrl}}/api/risk/{{orderId}}
Gateway ops (no business token needed for health)
Name	Method	Path
Gateway health
GET
{{baseUrl}}/actuator/health
8. Happy flow (Postman order)
Register → 201 (or 409 if email already exists — skip to login).
Login → 200, Tests save token.
Create order — laptop, 99.99 → 201, status: PENDING_RISK, save orderId.
Wait 10–30 seconds (Kafka saga: inventory + payment + AI risk).
My orders → expect final status: APPROVED, HELD, or REJECTED; inventory/payment/risk fields filled.
GET payment {{orderId}} → COMPLETED if payment OK.
GET risk {{orderId}} → decision + score (needs GROQ_API_KEY + ai-risk up).
GET notifications {{orderId}} → row after terminal order event (ORDER_APPROVED / HELD / REJECTED).
What “working” looks like

Happy: status = APPROVED (or HELD if AI says hold), inventoryStatus = OK, paymentStatus = OK, riskDecision set.
Still PENDING_RISK → wait longer or check ai-risk / Kafka logs.
9. Unhappy flows (Postman)
A. No token (gateway)
Request	Expected
GET {{baseUrl}}/api/orders/mine — No Authorization
401 — missing/malformed header
B. Bad token
Request	Expected
GET {{baseUrl}}/api/orders/mine — Bearer not-a-real-jwt
401 — invalid/expired JWT
C. Inventory rejected (deterministic)
Create order:


{
  "sku": "SKU-HEADSET-003",
  "quantity": 100,
  "totalAmount": 49.99
}
Only 5 units in stock → saga → order tends toward REJECTED (inventoryStatus = REJECTED).

D. Payment rejected (deterministic)
Create order:


{
  "sku": "SKU-LAPTOP-001",
  "quantity": 1,
  "totalAmount": 9999.99
}
Over 5000 (MAX_AUTO_APPROVE_AMOUNT) → payment always fails → order REJECTED.

E. Payment rejected (random ~10%)
Same happy body (99.99) — sometimes paymentStatus = REJECTED. Retry create order.

F. Wrong role
Request	Expected
GET {{baseUrl}}/api/orders as CUSTOMER
403 — ADMIN only
G. AI risk down / no Groq key
Order may stay PENDING_RISK with riskDecision: null until ai-risk runs. Fix: set GROQ_API_KEY, restart ai-risk-service.

10. Same flows with curl (optional)

export BASE=http://localhost:8080
curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@codekerdos.in","password":"secret123","fullName":"Jane Doe"}'
export TOKEN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@codekerdos.in","password":"secret123"}' | jq -r .token)
curl -s -X POST "$BASE/api/orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sku":"SKU-LAPTOP-001","quantity":1,"totalAmount":99.99}' | jq .
sleep 15
curl -s "$BASE/api/orders/mine" -H "Authorization: Bearer $TOKEN" | jq .
Unhappy — no token


curl -i "$BASE/api/orders/mine"
11. Saga reminder (why you wait)
POST /api/orders returns immediately with PENDING_RISK. Inventory, payment, and AI risk react on Kafka topic order-events and reply asynchronously. Poll GET /api/orders/mine until status changes.

12. Troubleshooting
Symptom	Likely cause
503 on gateway
Order service not registered in Eureka yet — wait / check docker logs order-service
401 with valid-looking token
JWT_SECRET mismatch between services — align .env
Stuck PENDING_RISK
ai-risk not up or missing GROQ_API_KEY
Payment random fail
~10% RANDOM_FAILURE_RATE — place order again

docker logs -f order-service
docker logs -f ai-risk-service

Copy from the title through the troubleshooting table. I’m in **Ask mode** — if you want this saved as a file in the repo, switch to **Agent mode** and say where to put it.
