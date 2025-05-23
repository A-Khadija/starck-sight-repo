import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * Stack Exchange API to Kafka Ingestion in Java
 *
 * This application:
 * 1. Fetches data from the Stack Exchange API
 * 2. Produces messages to Kafka topics for the StackSights pipeline
 * 3. Handles rate limiting, authentication, and error handling
 * 4. Provides both questions and tag trends data
 */
public class StackExchangeToKafka {

    // Configuration
    private static final String STACK_API_BASE_URL = "https://api.stackexchange.com/2.3";
    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:29092";
    private static final String QUESTION_TOPIC = "stackoverflow-questions";
    private static final String TRENDS_TOPIC = "stackoverflow-trends";
    private static final String SITE = "stackoverflow";
    private static final int PAGE_SIZE = 100;
    private static final int BACKFILL_DAYS = 7;
    private static final int DEFAULT_FETCH_INTERVAL = 300; // seconds
    
    // Stack Exchange API key (optional)
    private static final String API_KEY = null;
    
    private final KafkaProducer<String, String> producer;
    private long lastFetchTime = -1;
    private int quotaRemaining = -1;
    private long backoffUntil = -1;
    
    /**
     * Constructor
     */
    public StackExchangeToKafka() {
        // Configure Kafka producer
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        this.producer = new KafkaProducer<>(props);
    }
    
    /**
     * Make a request to the Stack Exchange API
     */
    private JSONObject makeRequest(String endpoint, Map<String, String> params) {
        try {
            // Check if we need to wait due to backoff
            long now = System.currentTimeMillis() / 1000;
            if (backoffUntil > 0 && now < backoffUntil) {
                long sleepTime = backoffUntil - now;
                System.out.println("Backing off for " + sleepTime + " seconds");
                Thread.sleep(sleepTime * 1000);
            }
            
            // Build URL
            StringBuilder urlBuilder = new StringBuilder(STACK_API_BASE_URL + "/" + endpoint + "?site=" + SITE);
            
            // Add parameters
            for (Map.Entry<String, String> entry : params.entrySet()) {
                urlBuilder.append("&").append(entry.getKey()).append("=").append(entry.getValue());
            }
            
            // Add API key if available
            if (API_KEY != null && !API_KEY.isEmpty()) {
                urlBuilder.append("&key=").append(API_KEY);
            }
            
            URL url = new URL(urlBuilder.toString());
            
            // Make the request
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response (handle gzip compression)
                BufferedReader in;
                if ("gzip".equals(connection.getContentEncoding())) {
                    in = new BufferedReader(new InputStreamReader(
                            new GZIPInputStream(connection.getInputStream())));
                } else {
                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                }
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                
                // Parse JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                
                // Update quota information
                if (jsonResponse.has("quota_remaining")) {
                    quotaRemaining = jsonResponse.getInt("quota_remaining");
                    System.out.println("API quota remaining: " + quotaRemaining);
                }
                
                // Handle backoff
                if (jsonResponse.has("backoff")) {
                    int backoffSeconds = jsonResponse.getInt("backoff");
                    backoffUntil = System.currentTimeMillis() / 1000 + backoffSeconds;
                    System.out.println("API requested backoff: " + backoffSeconds + " seconds");
                }
                
                return jsonResponse;
            } else {
                System.err.println("API request failed with status code: " + responseCode);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("Error making API request: " + e.getMessage());
            e.printStackTrace();
            
            // Sleep a bit before retrying
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            
            return null;
        }
    }
    
    /**
     * Get questions from Stack Overflow
     */
    private JSONObject getQuestions(Long fromDate, Long toDate, int page) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("pagesize", String.valueOf(PAGE_SIZE));
        params.put("sort", "creation");
        params.put("order", "desc");
        params.put("filter", "!9_bDE(fI5"); // Include tags and answers
        
        if (fromDate != null) {
            params.put("fromdate", String.valueOf(fromDate));
        }
        
        if (toDate != null) {
            params.put("todate", String.valueOf(toDate));
        }
        
        return makeRequest("questions", params);
    }
    
    /**
     * Get tags from Stack Overflow
     */
    private JSONObject getTags(int page) {
        Map<String, String> params = new HashMap<>();
        params.put("page", String.valueOf(page));
        params.put("pagesize", String.valueOf(PAGE_SIZE));
        params.put("sort", "popular");
        params.put("order", "desc");
        
        return makeRequest("tags", params);
    }
    
    /**
     * Send data to Kafka
     */
    private boolean sendToKafka(String topic, String key, String data) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, data);
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("Error sending message to Kafka: " + exception.getMessage());
                } else {
                    System.out.println("Message sent to " + metadata.topic() + 
                            " partition " + metadata.partition() + 
                            " offset " + metadata.offset());
                }
            });
            return true;
        } catch (Exception e) {
            System.err.println("Error sending message to Kafka: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Calculate the time range for fetching questions
     */
    private Map<String, Long> getTimeRange() {
        long now = System.currentTimeMillis() / 1000;
        
        long fromDate;
        if (lastFetchTime == -1) {
            // First run, get historical data
            fromDate = now - (BACKFILL_DAYS * 86400);
            System.out.println("First run, fetching data for the past " + BACKFILL_DAYS + " days");
        } else {
            // Subsequent runs, get data since last fetch
            fromDate = lastFetchTime;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println("Fetching data since " + sdf.format(new Date(fromDate * 1000)));
        }
        
        lastFetchTime = now;
        
        Map<String, Long> result = new HashMap<>();
        result.put("fromDate", fromDate);
        result.put("toDate", now);
        
        return result;
    }
    
    /**
     * Fetch questions and ingest into Kafka
     */
    public void fetchAndIngestQuestions() {
        Map<String, Long> timeRange = getTimeRange();
        Long fromDate = timeRange.get("fromDate");
        Long toDate = timeRange.get("toDate");
        
        // Fetch questions
        int page = 1;
        boolean hasMore = true;
        int questionsCount = 0;
        
        while (hasMore) {
            JSONObject response = getQuestions(fromDate, toDate, page);
            
            if (response != null && response.has("items")) {
                JSONArray items = response.getJSONArray("items");
                hasMore = response.optBoolean("has_more", false);
                
                // Send each question to Kafka
                for (int i = 0; i < items.length(); i++) {
                    JSONObject question = items.getJSONObject(i);
                    String questionId = String.valueOf(question.getLong("question_id"));
                    boolean success = sendToKafka(QUESTION_TOPIC, questionId, question.toString());
                    if (success) {
                        questionsCount++;
                    }
                }
                
                System.out.println("Processed " + items.length() + " questions (page " + page + ")");
                page++;
                
                // Check if we should continue or stop due to quota limits
                if (quotaRemaining != -1 && quotaRemaining < 10) {
                    System.out.println("API quota running low, stopping fetch");
                    break;
                }
                
                // Small delay between requests to be nice to the API
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                System.err.println("Failed to fetch questions or empty response");
                hasMore = false;
            }
        }
        
        System.out.println("Total questions sent to Kafka: " + questionsCount);
    }
    
    /**
     * Fetch tag trends and ingest into Kafka
     */
    public void fetchAndIngestTrends() {
        // Fetch popular tags
        JSONObject response = getTags(1);
        
        if (response != null && response.has("items")) {
            JSONArray items = response.getJSONArray("items");
            int trendsCount = 0;
            
            // Send each tag trend to Kafka
            for (int i = 0; i < items.length(); i++) {
                JSONObject tag = items.getJSONObject(i);
                
                JSONObject trendData = new JSONObject();
                trendData.put("tag", tag.getString("name"));
                trendData.put("count", tag.getInt("count"));
                
                String tagName = tag.getString("name");
                boolean success = sendToKafka(TRENDS_TOPIC, tagName, trendData.toString());
                if (success) {
                    trendsCount++;
                }
            }
            
            System.out.println("Total tag trends sent to Kafka: " + trendsCount);
        } else {
            System.err.println("Failed to fetch tags or empty response");
        }
    }
    
    /**
     * Run the ingestion process continuously
     */
    public void runIngestion(int interval) throws InterruptedException {
        System.out.println("Starting Stack Exchange API to Kafka ingestion (interval: " + interval + "s)");
        
        try {
            while (true) {
                long startTime = System.currentTimeMillis();
                
                System.out.println("Fetching questions");
                fetchAndIngestQuestions();
                
                System.out.println("Fetching tag trends");
                fetchAndIngestTrends();
                
                // Calculate sleep time to maintain the interval
                long elapsed = System.currentTimeMillis() - startTime;
                long sleepTime = Math.max(0, interval * 1000 - elapsed);
                
                System.out.println("Fetch completed in " + (elapsed / 1000) + "s, sleeping for " + 
                        (sleepTime / 1000) + "s");
                TimeUnit.MILLISECONDS.sleep(sleepTime);
            }
        } finally {
            producer.close();
            System.out.println("Producer closed");
        }
    }
    
    /**
     * Print usage information
     */
    private static void printUsage() {
        System.out.println("Usage: java StackExchangeToKafka [--fetch-interval SECONDS]");
        System.out.println("  --fetch-interval SECONDS  Interval between API fetches in seconds (default: " + 
                DEFAULT_FETCH_INTERVAL + ")");
    }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        int fetchInterval = DEFAULT_FETCH_INTERVAL;
        
        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if ("--fetch-interval".equals(args[i]) && i + 1 < args.length) {
                try {
                    fetchInterval = Integer.parseInt(args[i + 1]);
                    i++; // Skip the next argument
                } catch (NumberFormatException e) {
                    System.err.println("Invalid fetch interval: " + args[i + 1]);
                    printUsage();
                    System.exit(1);
                }
            } else {
                System.err.println("Unknown argument: " + args[i]);
                printUsage();
                System.exit(1);
            }
        }
        
        // Create and run the ingestion
        StackExchangeToKafka ingestion = new StackExchangeToKafka();
        try {
            ingestion.runIngestion(fetchInterval);
        } catch (InterruptedException e) {
            System.out.println("Ingestion interrupted, shutting down");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}