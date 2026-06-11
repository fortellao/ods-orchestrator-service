# Roadmap

## 1. Partial Reservation Compensation

**Implement the partial reservation flow using `ReservedItem`**

`ProductEvent.reservedItems` and the `ReservedItem` type are intentional placeholders for a planned flow where product service partially fulfills an order. The compensation path (RELEASE) currently sends only the `orderId` and relies on the product service to resolve the full reservation. When partial reservations are introduced, the orchestrator will need to forward `reservedItems` on the RELEASE command so the product service knows exactly what to roll back.

This is a cross-service contract change — coordinate with the product service before implementing.