#!/bin/bash

BOOTSTRAP_SERVER="localhost:9092"

TOPICS=(
  topic-order-event
  topic-order-command
  topic-inventory-event
  topic-inventory-command
  topic-payment-event
  topic-payment-command
)

PIDS=()

cleanup() {
  echo ""
  echo "Stopping consumers..."
  for pid in "${PIDS[@]}"; do
    kill "$pid" 2>/dev/null
  done
  exit 0
}

trap cleanup SIGINT SIGTERM

for topic in "${TOPICS[@]}"; do
  kafka-console-consumer \
    --bootstrap-server "$BOOTSTRAP_SERVER" \
    --topic "$topic" \
    --from-beginning 2>/dev/null \
    | awk -v t="[$topic]" '{ print t, $0 }' &
  PIDS+=($!)
done

echo "Monitoring ${#TOPICS[@]} topics on $BOOTSTRAP_SERVER. Press Ctrl+C to stop."
wait