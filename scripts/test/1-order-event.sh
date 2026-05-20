#!/bin/bash

echo '{"customerId":"cust-8821","paymentId":"pay-4493","items":[{"productId":"prod-001","quantity":2},{"productId":"prod-047","quantity":1}]}' | \
  kafka-console-producer --bootstrap-server localhost:9092 --topic topic-order-event