import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * HBase Table Creator for StackSights
 * 
 * This utility creates the HBase tables needed for the StackSights pipeline:
 * - stackoverflow_qna: Stores questions and answers
 * - stackoverflow_trends: Stores trend metrics
 * - stackoverflow_tag_index: Maps tags to question IDs
 */
public class HBaseTableCreator {

    // Configuration
    private static final String HBASE_ZOOKEEPER_QUORUM = "localhost";
    private static final String HBASE_ZOOKEEPER_CLIENT_PORT = "2181";
    
    // Table names
    private static final String QNA_TABLE = "stackoverflow_qna";
    private static final String TRENDS_TABLE = "stackoverflow_trends";
    private static final String TAG_INDEX_TABLE = "stackoverflow_tag_index";
    
    // Column family names
    private static final byte[] QUESTION_CF = Bytes.toBytes("question");
    private static final byte[] ANSWERS_CF = Bytes.toBytes("answers");
    private static final byte[] TOP_ANSWERS_CF = Bytes.toBytes("top_answers");
    private static final byte[] TREND_CF = Bytes.toBytes("trend");
    private static final byte[] QUESTION_IDS_CF = Bytes.toBytes("question_ids");
    
    /**
     * Create an HBase connection
     */
    private static Connection createConnection() throws IOException {
        Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.quorum", HBASE_ZOOKEEPER_QUORUM);
        config.set("hbase.zookeeper.property.clientPort", HBASE_ZOOKEEPER_CLIENT_PORT);
        
        return ConnectionFactory.createConnection(config);
    }
    
    /**
     * Create the stackoverflow_qna table
     */
    private static void createQnaTable(Admin admin) throws IOException {
        TableName tableName = TableName.valueOf(QNA_TABLE);
        
        if (admin.tableExists(tableName)) {
            System.out.println("Table " + QNA_TABLE + " already exists");
            return;
        }
        
        TableDescriptorBuilder tableBuilder = TableDescriptorBuilder.newBuilder(tableName);
        
        // Add column families
        ColumnFamilyDescriptorBuilder questionCfBuilder = ColumnFamilyDescriptorBuilder.newBuilder(QUESTION_CF);
        questionCfBuilder.setMaxVersions(1);
        tableBuilder.setColumnFamily(questionCfBuilder.build());
        
        ColumnFamilyDescriptorBuilder answersCfBuilder = ColumnFamilyDescriptorBuilder.newBuilder(ANSWERS_CF);
        answersCfBuilder.setMaxVersions(1);
        tableBuilder.setColumnFamily(answersCfBuilder.build());
        
        ColumnFamilyDescriptorBuilder topAnswersCfBuilder = ColumnFamilyDescriptorBuilder.newBuilder(TOP_ANSWERS_CF);
        topAnswersCfBuilder.setMaxVersions(1);
        tableBuilder.setColumnFamily(topAnswersCfBuilder.build());
        
        // Create the table
        admin.createTable(tableBuilder.build());
        System.out.println("Created table " + QNA_TABLE);
    }
    
    /**
     * Create the stackoverflow_trends table
     */
    private static void createTrendsTable(Admin admin) throws IOException {
        TableName tableName = TableName.valueOf(TRENDS_TABLE);
        
        if (admin.tableExists(tableName)) {
            System.out.println("Table " + TRENDS_TABLE + " already exists");
            return;
        }
        
        TableDescriptorBuilder tableBuilder = TableDescriptorBuilder.newBuilder(tableName);
        
        // Add column family
        ColumnFamilyDescriptorBuilder trendCfBuilder = ColumnFamilyDescriptorBuilder.newBuilder(TREND_CF);
        trendCfBuilder.setMaxVersions(1);
        tableBuilder.setColumnFamily(trendCfBuilder.build());
        
        // Create the table
        admin.createTable(tableBuilder.build());
        System.out.println("Created table " + TRENDS_TABLE);
    }
    
    /**
     * Create the stackoverflow_tag_index table
     */
    private static void createTagIndexTable(Admin admin) throws IOException {
        TableName tableName = TableName.valueOf(TAG_INDEX_TABLE);
        
        if (admin.tableExists(tableName)) {
            System.out.println("Table " + TAG_INDEX_TABLE + " already exists");
            return;
        }
        
        TableDescriptorBuilder tableBuilder = TableDescriptorBuilder.newBuilder(tableName);
        
        // Add column family
        ColumnFamilyDescriptorBuilder questionIdsCfBuilder = ColumnFamilyDescriptorBuilder.newBuilder(QUESTION_IDS_CF);
        questionIdsCfBuilder.setMaxVersions(1);
        tableBuilder.setColumnFamily(questionIdsCfBuilder.build());
        
        // Create the table
        admin.createTable(tableBuilder.build());
        System.out.println("Created table " + TAG_INDEX_TABLE);
    }
    
    /**
     * Create all tables
     */
    private static void createAllTables() {
        try (Connection connection = createConnection();
             Admin admin = connection.getAdmin()) {
            
            // Create tables
            createQnaTable(admin);
            createTrendsTable(admin);
            createTagIndexTable(admin);
            
            // List tables
            List<TableDescriptor> tables = admin.listTableDescriptors();
            System.out.println("\nAvailable HBase tables:");
            for (TableDescriptor table : tables) {
                System.out.println("- " + table.getTableName().getNameAsString());
            }
            
        } catch (IOException e) {
            System.err.println("Error creating HBase tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Delete tables if they exist (for clean setup)
     */
    private static void deleteTables(String... tableNames) {
        try (Connection connection = createConnection();
             Admin admin = connection.getAdmin()) {
            
            for (String tableName : tableNames) {
                TableName tn = TableName.valueOf(tableName);
                if (admin.tableExists(tn)) {
                    if (admin.isTableEnabled(tn)) {
                        admin.disableTable(tn);
                    }
                    admin.deleteTable(tn);
                    System.out.println("Deleted table " + tableName);
                } else {
                    System.out.println("Table " + tableName + " does not exist");
                }
            }
            
        } catch (IOException e) {
            System.err.println("Error deleting HBase tables: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Print usage information
     */
    private static void printUsage() {
        System.out.println("Usage: java HBaseTableCreator [--clean]");
        System.out.println("  --clean  Delete tables before creating them");
    }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        boolean clean = false;
        
        // Parse command line arguments
        for (String arg : args) {
            if ("--clean".equals(arg)) {
                clean = true;
            } else {
                System.err.println("Unknown argument: " + arg);
                printUsage();
                System.exit(1);
            }
        }
        
        // Delete tables if requested
        if (clean) {
            System.out.println("Cleaning up existing tables...");
            deleteTables(QNA_TABLE, TRENDS_TABLE, TAG_INDEX_TABLE);
        }
        
        // Create tables
        System.out.println("Creating HBase tables for StackSights...");
        createAllTables();
        
        System.out.println("\nHBase setup complete!");
    }
}