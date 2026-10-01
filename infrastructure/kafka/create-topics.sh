#!/usr/bin/env bash
#
# Creates the ShopSphere event topics with an explicit partition count.
#
# Kafka's auto-topic-creation would otherwise pick broker-default partitions on first publish,
# which silently makes ordering per key an accident of the broker version rather than a decision.
# Partitioning by event type is safe here: every producer keys its message on the order ID, so
# all events for one order land on the same partition and are therefore ordered.
#
# Run by the `kafka-init` Compose service. Executing it twice is harmless.

set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP:-kafka:9092}"
PARTITIONS="${KAFKA_TOPIC_PARTITIONS:-3}"
REPLICATION="${KAFKA_REPLICATION_FACTOR:-1}"
TIMEOUT="${KAFKA_INIT_TIMEOUT:-60}"

TOPICS=(
  order.created
  inventory.reserved
  inventory.insufficient
  payment.completed
  payment.failed
  order.shipped
)

echo "Waiting for Kafka at ${BOOTSTRAP} (timeout ${TIMEOUT}s)"
for _ in $(seq 1 "${TIMEOUT}"); do
  if kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" --list >/dev/null 2>&1; then
    echo "Kafka is available."
    break
  fi
  sleep 1
done

if ! kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" --list >/dev/null 2>&1; then
  echo "Kafka did not become available within ${TIMEOUT}s" >&2
  exit 1
fi

for topic in "${TOPICS[@]}"; do
  if kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" --list 2>/dev/null | grep -qx "${topic}"; then
    echo "Topic ${topic} already exists, leaving it unchanged."
    continue
  fi
  kafka-topics.sh \
    --bootstrap-server "${BOOTSTRAP}" \
    --create \
    --topic "${topic}" \
    --partitions "${PARTITIONS}" \
    --replication-factor "${REPLICATION}" \
    --config cleanup.policy=delete \
    --config retention.ms=604800000
  echo "Created ${topic} (partitions=${PARTITIONS}, replication=${REPLICATION})."
done

echo "ShopSphere topics are ready:"
kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" --list | grep -E '^(order|inventory|payment)\.'