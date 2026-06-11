# Kafka Event Examples

Events and commands produced/consumed by the Order Orchestration Service, grouped by topic.

---

## `order-event` — OrderEvent (inbound)

Published by the order-service to kick off a new order.

**Pretty**
```json
{
  "customerId": "cust-8821",
  "paymentId": "pay-4493",
  "items": [
    {
      "productId": "prod-001",
      "quantity": 2
    },
    {
      "productId": "prod-047",
      "quantity": 1
    }
  ]
}
```

**Compressed**
```json
{"customerId":"cust-8821","paymentId":"pay-4493","items":[{"productId":"prod-001","quantity":2},{"productId":"prod-047","quantity":1}]}
```

---

## `product-command` — ProductCommand (outbound)

Published by the orchestrator to request a checkout or release from product-service.

### Checkout

**Pretty**
```json
{
  "orderId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "operation": "CHECKOUT",
  "items": [
    {
      "productId": "prod-001",
      "quantity": 2
    },
    {
      "productId": "prod-047",
      "quantity": 1
    }
  ]
}
```

**Compressed**
```json
{"orderId":"f47ac10b-58cc-4372-a567-0e02b2c3d479","operation":"CHECKOUT","items":[{"productId":"prod-001","quantity":2},{"productId":"prod-047","quantity":1}]}
```

### Release (compensation)

Sent when payment fails. No items needed — product-service resolves the reservation by orderId.

**Pretty**
```json
{
  "orderId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "operation": "RELEASE"
}
```

**Compressed**
```json
{"orderId":"f47ac10b-58cc-4372-a567-0e02b2c3d479","operation":"RELEASE"}
```

---

## `product-event` — ProductEvent (inbound)

Response from product-service after a checkout attempt. Includes the pre-calculated total price on success.

### Success

**Pretty**
```json
{
  "orderId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "success": true,
  "totalPrice": 74.48,
  "reservedItems": [
    {
      "productId": "prod-001",
      "quantity": 2,
      "unitPrice": 29.99
    },
    {
      "productId": "prod-047",
      "quantity": 1,
      "unitPrice": 14.50
    }
  ]
}
```

**Compressed**
```json
{"orderId":"f47ac10b-58cc-4372-a567-0e02b2c3d479","success":true,"totalPrice":74.48,"reservedItems":[{"productId":"prod-001","quantity":2,"unitPrice":29.99},{"productId":"prod-047","quantity":1,"unitPrice":14.50}]}
```

### Failure

**Pretty**
```json
{
  "orderId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "success": false,
  "reservedItems": []
}
```

**Compressed**
```json
{"orderId":"f47ac10b-58cc-4372-a567-0e02b2c3d479","success":false,"reservedItems":[]}
```

---

## `payment-event` — PaymentEvent (inbound)

Response from payment-service after validating the payment.

### Success

**Pretty**
```json
{
  "orderId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "success": true
}
```

**Compressed**
```json
{"orderId":"f47ac10b-58cc-4372-a567-0e02b2c3d479","success":true}
```

### Failure

**Pretty**
```json
{
  "orderId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "success": false
}
```

**Compressed**
```json
{"orderId":"f47ac10b-58cc-4372-a567-0e02b2c3d479","success":false}
```