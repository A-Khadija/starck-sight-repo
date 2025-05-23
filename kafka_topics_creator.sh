#!/bin/bash

# Script to create Kafka topics for StackSights

KAFKA_HOME=${KAFKA_HOME:-""}
BOOTSTRAP_SERVER="localhost:29092"
PARTITIONS=3
REPLICATION_FACTOR=1

# Verify Kafka is running
echo "Verifying Kafka connection..."
${KAFKA_HOME}/bin/kafka-topics.sh --bootstrap-server ${BOOTSTRAP_SERVER} --list > /dev/null 2>&1
if [ $? -ne 0 ]; then
  echo "Error: Cannot connect to Kafka. Make sure Kafka is running on ${BOOTSTRAP_SERVER}."
  exit 1
fi

# Create stackoverflow-questions topic
echo "Creating stackoverflow-questions topic..."
${KAFKA_HOME}/bin/kafka-topics.sh --create \
  --topic stackoverflow-questions \
  --bootstrap-server ${BOOTSTRAP_SERVER} \
  --partitions ${PARTITIONS} \
  --replication-factor ${REPLICATION_FACTOR} \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --if-not-exists

# Create stackoverflow-trends topic
echo "Creating stackoverflow-trends topic..."
${KAFKA_HOME}/bin/kafka-topics.sh --create \
  --topic stackoverflow-trends \
  --bootstrap-server ${BOOTSTRAP_SERVER} \
  --partitions ${PARTITIONS} \
  --replication-factor ${REPLICATION_FACTOR} \
  --config retention.ms=604800000 \
  --config cleanup.policy=delete \
  --if-not-exists

# List all topics
echo -e "\nAvailable Kafka topics:"
${KAFKA_HOME}/bin/kafka-topics.sh --list --bootstrap-server ${BOOTSTRAP_SERVER}

echo -e "\nKafka topics setup complete!"