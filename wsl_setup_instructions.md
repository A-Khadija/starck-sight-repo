# StackSight Java Pipeline Setup for WSL

It looks like you're trying to run the Java pipeline in WSL (Windows Subsystem for Linux), but the script file isn't found. Let me provide you with step-by-step instructions to set up and run the pipeline in your WSL environment.

## Setup Instructions for WSL

1. **First, make sure you're in the correct directory:**
   ```bash
   cd /mnt/c/Users/hp/OneDrive/Documents/stacksight
   ```

2. **Copy all the Java files to this directory**
   You need to make sure the following files are in your directory:
   - HBaseTableCreator.java
   - StackExchangeToKafka.java
   - StackSightsStreaming.java
   - pom.xml (rename pom_complete.xml to pom.xml if needed)

3. **Create the shell scripts in the correct directory:**
   ```bash
   # Create the run script
   nano run_java_pipeline_all.sh
   ```

   Copy and paste the content from the run_java_pipeline_all.sh file I provided.
   Press CTRL+X, then Y, then Enter to save.

   ```bash
   # Create the Kafka topics script
   nano kafka_topics_creator.sh
   ```

   Copy and paste the content from the kafka_topics_creator.sh file I provided.
   Press CTRL+X, then Y, then Enter to save.

4. **Make the scripts executable:**
   ```bash
   chmod +x run_java_pipeline_all.sh
   chmod +x kafka_topics_creator.sh
   ```

5. **Update the Kafka path in both scripts:**
   ```bash
   # Edit the run script
   nano run_java_pipeline_all.sh
   ```

   Find the line `KAFKA_HOME=${KAFKA_HOME:-"/path/to/kafka"}` and update it to your actual Kafka path, which might be something like:
   ```
   KAFKA_HOME=${KAFKA_HOME:-"/mnt/c/Users/hp/kafka"}
   ```
   (Adjust according to your actual Kafka installation location)

   Do the same for kafka_topics_creator.sh.

## Running the Pipeline in WSL

1. **Make sure Java, Maven, Kafka, and HBase are properly installed in your WSL environment:**
   ```bash
   # Check Java
   java -version

   # Check Maven
   mvn -version

   # Check Kafka (if Kafka server is running)
   ${KAFKA_HOME}/bin/kafka-topics.sh --list --bootstrap-server localhost:29092
   ```

2. **Build the project:**
   ```bash
   mvn clean package
   ```

3. **Run the complete pipeline:**
   ```bash
   ./run_java_pipeline_all.sh
   ```

   Or run components individually as described in the java_pipeline_complete_instructions.md file.

## Common WSL-Specific Issues

1. **Path issues:** WSL paths are different from Windows paths. Make sure all paths are in Linux format (e.g., `/mnt/c/...` not `C:\...`).

2. **Line endings:** If you edited files in Windows, they might have Windows-style line endings. Fix this with:
   ```bash
   dos2unix run_java_pipeline_all.sh
   dos2unix kafka_topics_creator.sh
   ```

3. **Firewall issues:** WSL might have trouble connecting to services. Make sure your Windows firewall allows the connections.

4. **Port forwarding:** If running services in WSL, make sure ports are properly forwarded.

5. **Network configuration:** For Kafka and HBase, you might need to use the WSL IP instead of localhost in some cases.

## Verifying Files

To check if all required files are in your directory:

```bash
ls -la
```

You should see all the Java files, shell scripts, and pom.xml listed.

If you need to create any missing files, you can use the `nano` editor as shown above, or use Visual Studio Code from Windows with the WSL extension to edit files directly in your WSL environment.