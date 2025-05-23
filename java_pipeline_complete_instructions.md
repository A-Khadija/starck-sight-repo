# Complete Java-Based StackSights Pipeline

I've rewritten the entire pipeline in Java to avoid Python-to-JVM issues. This implementation includes all three major components of the pipeline:

1. **HBase table creation**
2. **Stack Exchange API to Kafka ingestion**
3. **Spark streaming processing**

## Files Overview

### Java Implementation Files

1. **`HBaseTableCreator.java`** - Creates the HBase tables with proper schema
2. **`StackExchangeToKafka.java`** - Fetches data from Stack Exchange API and writes to Kafka
3. **`StackSightsStreaming.java`** - Processes data from Kafka and writes to HBase

### Support Files

1. **`pom_complete.xml`** - Maven project file with all required dependencies (rename to `pom.xml`)
2. **`kafka_topics_creator.sh`** - Shell script to create the Kafka topics
3. **`run_java_pipeline_all.sh`** - Master script to run the entire pipeline

## Setup Instructions

1. **Place all files in your project directory:**
   ```
   /mnt/c/Users/htagi/kafka-spark-hbase/
   ├── HBaseTableCreator.java
   ├── StackExchangeToKafka.java
   ├── StackSightsStreaming.java
   ├── pom.xml (renamed from pom_complete.xml)
   ├── kafka_topics_creator.sh
   └── run_java_pipeline_all.sh
   ```

2. **Update configuration in the scripts:**
   - In `kafka_topics_creator.sh` and `run_java_pipeline_all.sh`, update the `KAFKA_HOME` variable to point to your Kafka installation

3. **Make the scripts executable:**
   ```bash
   chmod +x kafka_topics_creator.sh run_java_pipeline_all.sh
   ```

## Running the Pipeline

### Option 1: Run Everything with One Script

The simplest way to run the entire pipeline is to use the master script:

```bash
./run_java_pipeline_all.sh
```

This script will:
1. Build all Java applications
2. Create HBase tables
3. Create Kafka topics
4. Start the Kafka ingestion in the background
5. Run the Spark streaming application

All logs will be saved in the `logs` directory.

### Option 2: Run Components Individually

If you prefer to run each component separately:

1. **Build the Java applications:**
   ```bash
   mvn clean package
   ```

2. **Create HBase tables:**
   ```bash
   java -cp target/stacksights-1.0-SNAPSHOT.jar HBaseTableCreator
   ```

3. **Create Kafka topics:**
   ```bash
   ./kafka_topics_creator.sh
   ```

4. **Start Kafka ingestion:**
   ```bash
   java -cp target/stacksights-1.0-SNAPSHOT.jar StackExchangeToKafka
   ```

5. **Run Spark streaming:**
   ```bash
   spark-submit \
     --class StackSightsStreaming \
     --master local[*] \
     --packages org.apache.spark:spark-sql-kafka-0-10_2.12:3.2.0 \
     --conf "spark.driver.memory=2g" \
     --conf "spark.executor.memory=2g" \
     target/stacksights-1.0-SNAPSHOT.jar
   ```

## Advantages of the Java Implementation

1. **Performance**: Native execution in the JVM without Python interpreter overhead
2. **Stability**: No Py4J bridge to cause communication errors
3. **Integration**: Direct access to Java APIs for HBase, Kafka, and Spark
4. **Memory Management**: Better control over memory usage in the JVM
5. **Consistency**: Same language used across all parts of the pipeline

## Troubleshooting

### HBase Connection Issues

If you encounter HBase connection issues:
- Verify HBase is running with `jps` and look for HMaster
- Check HBase configuration in `/etc/hbase/conf/hbase-site.xml` 
- Make sure the ZooKeeper quorum settings in the Java files match your environment

### Kafka Connection Issues

If you encounter Kafka connection issues:
- Verify Kafka is running with `jps` and look for Kafka
- Test the connection with `kafka-topics.sh --list --bootstrap-server localhost:29092`
- Make sure the bootstrap server settings in the Java files match your environment

### Out of Memory Errors

If you encounter out of memory errors:
- Increase Java heap size with `-Xmx` option 
- Adjust Spark memory configuration in the `spark-submit` command
- Consider reducing batch sizes in the Java files

## Customization

You can customize the behavior by modifying these variables:
- In `HBaseTableCreator.java`: Table names and column family names
- In `StackExchangeToKafka.java`: API parameters, fetch interval, and backfill days
- In `StackSightsStreaming.java`: Batch sizes, watermark duration, and window size