#!/bin/bash

# Usage: ./2-product-event.sh <orderId> [--fail]
#   orderId — from topic-product-command after running 1-order-event.sh
#   --fail  — send a failure response instead of success

BOOTSTRAP_SERVER="localhost:9092"
TOPIC="topic-product-event"

ORDER_ID="$1"
FAIL=false

if [[ -z "$ORDER_ID" ]]; then
  echo "Usage: $0 <orderId> [--fail]"
  exit 1
fi

if [[ "$2" == "--fail" ]]; then
  FAIL=true
fi

if [[ "$FAIL" == true ]]; then
  MESSAGE="{\"orderId\":\"$ORDER_ID\",\"success\":false,\"reservedItems\":[]}"
else
  MESSAGE="{\"orderId\":\"$ORDER_ID\",\"success\":true,\"totalPrice\":74.48,\"reservedItems\":[{\"productId\":\"prod-001\",\"quantity\":2,\"unitPrice\":29.99},{\"productId\":\"prod-047\",\"quantity\":1,\"unitPrice\":14.50}]}"
fi

echo "$MESSAGE" | kafka-console-producer --bootstrap-server "$BOOTSTRAP_SERVER" --topic "$TOPIC"