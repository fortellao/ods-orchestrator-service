# TODO

Technical debt and production-readiness items, ordered by priority.

---

## 1. Resilience & Error Handling

**Dead letter queue for unprocessable messages**

Both `OrchestratorKafkaListener` and `OrchestratorKafkaPublisher` swallow all exceptions and log them. A malformed inbound message is silently committed and lost; a failed outbound publish is silently dropped. In production this causes invisible data loss with no recovery path.

Options:
- `@RetryableTopic` on each `@KafkaListener` method for transient failures, with a DLT for poison messages.
- For publish failures, surface the exception so the listener's error handler can decide (retry vs. DLT).

**Idempotency guard on order creation**

Kafka delivers at-least-once. If the same `OrderEvent` is received twice, two separate orders are created with different UUIDs. A correlation ID (e.g. a client-provided `requestId` on `OrderEvent`) should be used as the store key, with a duplicate check before persisting.

---

## 2. Persistence

**Replace `InMemorySagaOrderStore` with a durable store**

The current `ConcurrentHashMap` implementation is a placeholder — all in-flight saga state is lost on restart. The `SagaOrderStore` interface is already designed for this swap. Implement a Redis or MongoDB adapter in `store/` and wire it in place of `InMemorySagaOrderStore`. Redis is the natural fit given the key-value access pattern and the need for TTL-based expiry of abandoned sagas.

---

## 3. Test Coverage

**Test unknown-order edge cases**

`onProductSuccess` and `onPaymentSuccess` both guard against events arriving for unknown orders (log + return), but neither path has a test. Add:

- `onProductEvent_onSuccess_unknownOrder_doesNothing`
- `onPaymentEvent_onSuccess_unknownOrder_doesNothing`

**Test idempotency once the guard is in place**

Once a correlation-ID-based duplicate check exists, add a test that delivers the same `OrderEvent` twice and asserts only one order is created and only one product command is sent.

---

## 4. Production Hardening

**Restrict Actuator endpoint exposure**

`management.endpoints.web.exposure.include: "*"` exposes all Actuator endpoints including env, beans, and heap dump. Restrict to `health,info,metrics,prometheus` (or whatever the observability stack requires) before deploying to any non-local environment.

**Add Micrometer metrics for saga state**

Emit counters for saga outcomes (orders confirmed, orders failed, compensation triggered) and a gauge for in-flight saga count. These feed directly into alerting and SLO dashboards. The OTel / Jaeger stack is already in place — adding Micrometer metrics completes the three pillars (traces, metrics, logs).