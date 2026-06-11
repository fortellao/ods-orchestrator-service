#!/bin/bash

BOOTSTRAP_SERVER="localhost:9092"
PRINT_HEADERS=false
FROM_BEGINNING=false

for arg in "$@"; do
  case $arg in
    --headers|-H)       PRINT_HEADERS=true ;;
    --from-beginning|-B) FROM_BEGINNING=true ;;
  esac
done

TOPICS=(
  topic-order-event
  topic-order-command
  topic-product-event
  topic-product-command
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

EXTRA_ARGS=()
[ "$FROM_BEGINNING" = "true" ]  && EXTRA_ARGS+=(--from-beginning)
[ "$PRINT_HEADERS"  = "true" ]  && EXTRA_ARGS+=(--formatter-property print.headers=true)

for topic in "${TOPICS[@]}"; do
  kafka-console-consumer \
    --bootstrap-server "$BOOTSTRAP_SERVER" \
    --topic "$topic" \
    "${EXTRA_ARGS[@]}" \
    2>/dev/null \
    | awk -v t="[$topic]" '{ print t, $0 }' &
  PIDS+=($!)
done

echo "Monitoring ${#TOPICS[@]} topics on $BOOTSTRAP_SERVER. Press Ctrl+C to stop."
wait
