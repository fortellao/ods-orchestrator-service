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
│   ├── inventory/ Inventory domain types (InventoryCommand, InventoryEvent, InventoryOperation, ReservedItem)
│   ├── order/     Order domain types (Order, OrderCommand, OrderEvent, OrderStatus, Item)
│   └── payment/   Payment domain types (PaymentCommand, PaymentEvent)
├── messaging/
│   ├── inbound/   Kafka listener (inbound adapter)
│   └── outbound/  Kafka publisher (outbound adapter)
├── store/         Order state adapters (InMemorySagaOrderStore — swap here for Redis/MongoDB)
├── config/        KafkaTopicsConfiguration
└── status/        Health/status endpoint
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

`InventoryCommand.operation` is either `CHECKOUT` (with items) or `RELEASE` (orderId only — inventory-service resolves the reservation by orderId).

## Architectural Decisions

- **Hexagonal / ports-and-adapters**: the saga core is decoupled from Kafka and storage. Inbound port: `OrchestratorEventHandler`. Outbound ports: `OrchestratorCommandPublisher`, `SagaOrderStore`.
- **`SagaOrderStore` is intentionally extracted**: the in-memory `ConcurrentHashMap` implementation is a placeholder. The migration path is Redis or MongoDB — implement the interface in `store/` and swap the Spring bean.
- **Inventory service owns `totalPrice`**: the orchestrator does not calculate order totals. `InventoryEvent` carries `totalPrice`, which is forwarded as-is to the payment command.
- **Constructor injection only**: no `@Autowired` on fields or constructors (redundant since Spring 4.3+).
- **Compensation scope**: only payment failure triggers inventory compensation. Inventory failure needs no compensation — no reservation was made. Never call `sendInventoryRelease` from the inventory failure path.

## Testing Approach

- Saga logic is unit-tested with `@ExtendWith(MockitoExtension.class)` — no `@SpringBootTest`.
- Mock `OrchestratorCommandPublisher`; use a real `InMemorySagaOrderStore` so stored order state can be asserted directly.
- Tests live in `src/test/java/com/fortellao/ods/orchestration/saga/`.
- Mockito is registered as an explicit JVM agent via the Surefire plugin in `pom.xml`. This is required on modern JDKs — Java 16+ restricts dynamic agent self-attachment, and future releases will disallow it entirely, breaking tests. Do not remove that Surefire configuration.

## What to Avoid

- Do not add business logic to `OrchestratorKafkaListener` — it is transport only (deserialize + delegate).
- Do not add Kafka, Jackson, or Spring imports to `OrderOrchestratorService`.
- Do not calculate derived values (e.g. totals) in the orchestrator — delegate to the owning service.
- Do not remove `ReservedItem` or `InventoryEvent.reservedItems` — they are intentionally kept as a placeholder for a planned partial reservation compensation flow. They will have no test coverage until that flow is implemented.