#!/bin/bash

# Comprehensive script to run the entire Java-based StackSights pipeline

# Configuration
KAFKA_HOME=${KAFKA_HOME:-"/path/to/kafka"}
BOOTSTRAP_SERVER="localhost:29092"
FETCH_INTERVAL=300  # seconds
LOG_DIR="logs"
CURRENT_DATE=$(date +"%Y-%m-%d_%H-%M-%S")

# Create logs directory
mkdir -p ${LOG_DIR}

# Step 1: Build the Java applications
echo "Building Java applications..."
mvn clean package

if [ $? -ne 0 ]; then
  echo "Build failed. Please check the errors above."
  exit 1
fi

# Step 2: Create HBase tables
echo -e "\n===== Creating HBase Tables ====="
java -cp target/stacksights-1.0-SNAPSHOT.jar HBaseTableCreator --clean 2>&1 | tee ${LOG_DIR}/hbase_setup_${CURRENT_DATE}.log

# Step 3: Create Kafka topics
echo -e "\n===== Creating Kafka Topics ====="
chmod +x kafka_topics_creator.sh
./kafka_topics_creator.sh 2>&1 | tee ${LOG_DIR}/kafka_topics_${CURRENT_DATE}.log

# Step 4: Start the Kafka ingestion in the background
echo -e "\n===== Starting Stack Exchange to Kafka Ingestion ====="
java -cp target/stacksights-1.0-SNAPSHOT.jar StackExchangeToKafka --fetch-interval ${FETCH_INTERVAL} > ${LOG_DIR}/kafka_ingestion_${CURRENT_DATE}.log 2>&1 &
KAFKA_PID=$!
echo "Kafka ingestion started with PID ${KAFKA_PID}"
echo "Logs available at ${LOG_DIR}/kafka_ingestion_${CURRENT_DATE}.log"

# Wait a bit to allow Kafka ingestion to start producing data
sleep 10
echo "Waiting for data to be produced to Kafka..."

# Step 5: Run the Spark streaming application
echo -e "\n===== Starting Spark Streaming Application ====="
echo "Logs available at ${LOG_DIR}/spark_streaming_${CURRENT_DATE}.log"
echo "Press Ctrl+C to stop the pipeline"

# Run with spark-submit
spark-submit \
  --class StackSightsStreaming \
  --master local[*] \
  --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.2.0 \
  --conf "spark.driver.memory=2g" \
  --conf "spark.executor.memory=2g" \
  target/stacksights-1.0-SNAPSHOT.jar 2>&1 | tee ${LOG_DIR}/spark_streaming_${CURRENT_DATE}.log

# When Spark streaming exits (after Ctrl+C), clean up
echo -e "\n===== Cleaning Up ====="
echo "Stopping Kafka ingestion process (PID ${KAFKA_PID})..."
kill ${KAFKA_PID}
wait ${KAFKA_PID} 2>/dev/null

echo -e "\nStackSights pipeline has been stopped."