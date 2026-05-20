#!/bin/bash

# Usage: ./3-payment-event.sh <orderId> [--fail]
#   orderId — from topic-payment-command after running 2-inventory-event.sh
#   --fail  — send a failure response (triggers inventory RELEASE + FAILED order)

BOOTSTRAP_SERVER="localhost:9092"
TOPIC="topic-payment-event"

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
  MESSAGE="{\"orderId\":\"$ORDER_ID\",\"success\":false}"
else
  MESSAGE="{\"orderId\":\"$ORDER_ID\",\"success\":true}"
fi

echo "$MESSAGE" | kafka-console-producer --bootstrap-server "$BOOTSTRAP_SERVER" --topic "$TOPIC"