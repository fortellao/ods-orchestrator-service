# Claude Instructions — Order Orchestration Service

## Build & Run

```bash
mvn spring-boot:run
```

Requires a running Kafka broker. Topic names are configured in `application.yml` under `orchestration.topics`.

## Package Structure — Follow This

```
orchestration/
├── saga/          Core saga logic + ports (OrchestratorEventHandler, OrchestratorCommandPublisher, SagaOrderStore)
├── domain/
│   ├── product/   Product domain types (ProductCommand, ProductEvent, ProductOperation, ReservedItem)
│   ├── order/     Order domain types (Order, OrderCommand, OrderEvent, OrderStatus, Item)
│   └── payment/   Payment domain types (PaymentCommand, PaymentEvent)
├── messaging/
│   ├── inbound/   Kafka listener (inbound adapter)
│   └── outbound/  Kafka publisher (outbound adapter)
├── store/         Order state adapters (InMemorySagaOrderStore — swap here for Redis/MongoDB)
├── config/        KafkaTopicsConfiguration
└── health/        KafkaHealthIndicator (Actuator HealthIndicator)
```

**Rule:** ports (interfaces) live in `saga/`. Adapters live in `messaging/` or `store/`. The saga core (`OrderOrchestratorService`) must not import anything from `messaging/`, `store/`, or `config/`.

## Compensation Flow (Payment Failure)

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

`ProductCommand.operation` is either `CHECKOUT` (with items) or `RELEASE` (orderId only — inventory-service resolves the reservation by orderId).

## Architectural Decisions

- **Hexagonal / ports-and-adapters**: the saga core is decoupled from Kafka and storage. Inbound port: `OrchestratorEventHandler`. Outbound ports: `OrchestratorCommandPublisher`, `SagaOrderStore`.
- **`SagaOrderStore` is intentionally extracted**: the in-memory `ConcurrentHashMap` implementation is a placeholder. The migration path is Redis or MongoDB — implement the interface in `store/` and swap the Spring bean.
- **Inventory service owns `totalPrice`**: the orchestrator does not calculate order totals. `ProductEvent` carries `totalPrice`, which is forwarded as-is to the payment command.
- **Constructor injection only**: no `@Autowired` on fields or constructors (redundant since Spring 4.3+).
- **Compensation scope**: only payment failure triggers inventory compensation. Inventory failure needs no compensation — no reservation was made. Never call `sendInventoryRelease` from the inventory failure path.
- **`KafkaHealthIndicator` uses a persistent `AdminClient`**: the client is created once at startup and reused across all health checks. On-demand creation was considered but rejected — Kafka logs the full config dump and metrics lifecycle on every create/close, which floods logs when Kubernetes probes every few seconds. The persistent client is kept quiet by `metadata.recovery.strategy=none`, which prevents rebootstrap from resetting the reconnect backoff counter (the default `rebootstrap` strategy resets the counter on every cycle, causing rapid reconnect storms when the broker is down). `KafkaHealthIndicator` is NOT auto-configured in Spring Boot 4.x — the custom implementation in `health/` is required. Health groups are defined manually in `application.yml` rather than via `management.health.probes.enabled=true` because the probes-enabled auto-created groups ignore custom `HealthIndicator` beans.

## Testing Approach

- Saga logic is unit-tested with `@ExtendWith(MockitoExtension.class)` — no `@SpringBootTest`.
- Mock `OrchestratorCommandPublisher`; use a real `InMemorySagaOrderStore` so stored order state can be asserted directly.
- Tests live in `src/test/java/com/fortellao/ods/orchestration/saga/`.
- Mockito is registered as an explicit JVM agent via the Surefire plugin in `pom.xml`. This is required on modern JDKs — Java 16+ restricts dynamic agent self-attachment, and future releases will disallow it entirely, breaking tests. Do not remove that Surefire configuration.

## What to Avoid

- Do not add business logic to `OrchestratorKafkaListener` — it is transport only (deserialize + delegate).
- Do not add Kafka, Jackson, or Spring imports to `OrderOrchestratorService`.
- Do not calculate derived values (e.g. totals) in the orchestrator — delegate to the owning service.
- Do not remove `ReservedItem` or `ProductEvent.reservedItems` — they are intentionally kept as a placeholder for a planned partial reservation compensation flow. They will have no test coverage until that flow is implemented.