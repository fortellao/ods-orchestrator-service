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

## Local Testing

### Prerequisites

- Docker (for Kafka)
- `kafka-console-producer` / `kafka-console-consumer` on your `PATH` (included in the [Confluent Platform CLI](https://docs.confluent.io/platform/current/installation/installing_cp/zip-tar.html) or any Kafka distribution)

### 1. Start Kafka

```bash
docker compose -f docker-compose-kafka.yaml up -d
```

Broker is available at `localhost:9092`.

### 2. Start the service

```bash
mvn spring-boot:run
```

### 3. Monitor all topics (separate terminal)

```bash
bash scripts/test/monitor-topics.sh
```

Every message printed by the scripts below will appear here, tagged with its topic name.

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
