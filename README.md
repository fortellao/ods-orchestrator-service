# Order Orchestration Service

This Spring Boot microservice is the central coordinator in a distributed order processing system. It orchestrates the saga/workflow across inventory, payment, and other downstream services via Kafka.

## Architecture

The service communicates exclusively through Kafka topics, configured in `application.yml` under `orchestration.topics` and bound via `KafkaTopicsConfiguration`.

| Topic | Direction | Purpose |
|---|---|---|
| `order-event` | inbound | Incoming order requests |
| `order-command` | outbound | Commands to order domain |
| `inventory-command` | outbound | Commands sent to inventory-api |
| `inventory-event` | inbound | Responses from inventory-api |
| `payment-command` | outbound | Commands sent to payment service |
| `payment-event` | inbound | Responses from payment service |

## Happy Path Order Flow

```
order-service          orchestrator           inventory-service      payment-service
     |                      |                        |                      |
     |-- order-event ------>|                        |                      |
     |                      |-- inventory-command -->|                      |
     |                      |   (checkout request)   |                      |
     |                      |<-- inventory-event ----|                      |
     |                      |   (reservation OK +    |                      |
     |                      |    total price)        |                      |
     |                      |                        |                      |
     |                      |-- payment-command --------------------------->|
     |                      |   (total price + payment details)             |
     |                      |<-- payment-event -----------------------------|
     |                      |   (payment validated)                         |
     |                      |                                               |
     |<-- order-command ----|                                               |
     |   (status: CONFIRMED)                                                |
```

### Steps

1. **Receive order** — `order-event` topic triggers `onOrderReceived(OrderEvent)`.
2. **Inventory checkout** — publish the `items` list to `inventory-command` requesting availability check and reservation.
3. **Await inventory response** — `inventory-event` returns the reservation result. On success, proceed.
4. **Payment validation** — forward the total price provided by inventory-service to `payment-command`.
5. **Await payment response** — `payment-event` returns payment confirmation. On success, proceed.
6. **Confirm order** — publish to `order-command` with the order in `CONFIRMED` status.

## Payment Failure — Compensation Flow

When payment is declined the orchestrator releases the inventory reservation before marking the order as failed.

```
order-service          orchestrator           inventory-service      payment-service
     |                      |                        |                      |
     |                      |<-- payment-event -----------------------------|
     |                      |                        |   (success: false)   |
     |                      |-- inventory-command -->|                      |
     |                      |   (operation: RELEASE) |                      |
     |<-- order-command ----|                        |                      |
     |   (status: FAILED)   |                        |                      |
```

> Inventory failure does not trigger compensation — if the inventory checkout fails no reservation was made.

## Planned

### Partial reservation compensation

`ProductEvent` already carries `reservedItems` (a list of `ReservedItem` with `productId`, `quantity`, and `unitPrice`) for cases where the inventory service can only partially fulfil an order. A future compensation flow will use this to handle partial reservations — releasing only the items that were successfully reserved rather than sending a blanket `RELEASE` command.

## Health & Observability

Spring Boot Actuator is enabled with full endpoint exposure. All endpoints are available under `/actuator`.

| Endpoint | Purpose |
|---|---|
| `GET /actuator/health/liveness` | Kubernetes liveness probe — reflects Spring application lifecycle state |
| `GET /actuator/health/readiness` | Kubernetes readiness probe — includes Kafka connectivity check |
| `GET /actuator/health` | Full health detail across all contributors |
| `GET /actuator/info` | Application metadata |
| `GET /actuator/metrics` | JVM and application metrics |
| `GET /actuator/env` | Resolved configuration properties |

The readiness probe returns `DOWN` if the Kafka broker is unreachable, causing Kubernetes to stop routing traffic to the pod until connectivity is restored.

The Kafka health check timeout is configurable via `management.health.kafka.timeout-ms` (default `1000` ms). Set this below the Kubernetes probe `timeoutSeconds` to avoid false negatives.

## Local Testing

There are three ways to run locally depending on your environment.

### Option A — Kafka only (fast iteration)

#### Prerequisites

- Docker
- `kafka-console-producer` / `kafka-console-consumer` on your `PATH` (included in the [Confluent Platform CLI](https://docs.confluent.io/platform/current/installation/installing_cp/zip-tar.html) or any Kafka distribution)

#### 1. Start Kafka

```bash
docker compose -f docker-compose-kafka.yaml up -d
```

Broker is available at `localhost:9092`.

#### 2. Start the service

```bash
mvn spring-boot:run
```

### Option B — Full stack with OpenTelemetry

Runs the service as a Docker container with the OTel Java agent attached, alongside an OTel Collector and Jaeger for trace visualisation.

```bash
docker compose -f docker-compose-otel.yaml up --build
```

| URL | Purpose |
| --- | --- |
| `http://localhost:8080` | Service |
| `http://localhost:16686` | Jaeger UI — view traces here |

The first build downloads Maven dependencies inside the container and is slow. Subsequent builds are fast unless `pom.xml` changes.

Teardown:

```bash
docker compose -f docker-compose-otel.yaml down
```

### Option C — Remote Kafka (DEV environment)

Points the service at an existing Kafka broker (e.g. a shared DEV cluster) without running one locally.

Create `src/main/resources/application-local.yml` (gitignored — never committed) with the properties you want to override:

```yaml
spring:
  kafka:
    bootstrap-servers: <broker-host>:<port>
```

Then activate the `local` profile in your IDE run configuration (`-Dspring.profiles.active=local`) or from the terminal:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Add any other environment-specific overrides to the same file — it will never be picked up by git.

---

### Monitor all topics (separate terminal)

```bash
bash scripts/monitor-topics.sh
```

Every message printed by the scripts below will appear here, tagged with its topic name.

By default only new messages are shown. Pass `--from-beginning` (or `-B`) to replay from the earliest offset:

```bash
bash scripts/monitor-topics.sh --from-beginning
```

Pass `--headers` (or `-H`) to also print Kafka message headers — useful for inspecting OTel trace context (`traceparent`, `tracestate`) propagated by the agent:

```bash
bash scripts/monitor-topics.sh --headers
```

Flags can be combined:

```bash
bash scripts/monitor-topics.sh --from-beginning --headers
```

### 4. Run through the happy path

**Step 1 — Send an order event** (simulates order-service)

```bash
bash scripts/test/1-order-event.sh
```

The orchestrator receives it, generates an `orderId`, and publishes an `inventory-command`. Copy the `orderId` from the monitor output on `[topic-inventory-command]`.

**Step 2 — Send an inventory success event** (simulates inventory-service)

```bash
bash scripts/test/2-inventory-event.sh <orderId>
```

The orchestrator receives the reservation confirmation and publishes a `payment-command`.

**Step 3 — Send a payment success event** (simulates payment-service)

```bash
bash scripts/test/3-payment-event.sh <orderId>
```

The orchestrator publishes an `order-command` with status `CONFIRMED` on `[topic-order-command]`.

### Failure scenarios

**Inventory failure** — order marked `FAILED`, no payment attempted, no compensation needed:

```bash
bash scripts/test/2-inventory-event.sh <orderId> --fail
```

**Payment failure** — orchestrator releases the inventory reservation then marks the order `FAILED`:

```bash
bash scripts/test/3-payment-event.sh <orderId> --fail
```

You should see an `inventory-command` with `"operation":"RELEASE"` on `[topic-inventory-command]`, followed by an `order-command` with `status: FAILED` on `[topic-order-command]`.

### Teardown

```bash
docker compose -f docker-compose-kafka.yaml down
```

---

## Domain Model

- `OrderEvent` — inbound DTO: `customerId`, `paymentId`, `List<Item>`
- `Order` — internal representation built from an `OrderEvent`
- `Item` — `productId` + `quantity`
