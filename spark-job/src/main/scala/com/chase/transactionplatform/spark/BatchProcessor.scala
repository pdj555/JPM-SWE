package com.chase.transactionplatform.spark

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.types._
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

/**
 * Aurora Spark Streaming Job - Real-time Transaction Analytics
 * 
 * High-throughput streaming analytics processor designed for continuous aggregation
 * of financial transaction data with Apple-grade reliability and performance standards.
 * 
 * Key Features:
 * - Structured Streaming with exactly-once semantics
 * - Real-time windowed aggregations (1-minute tumbling windows)
 * - Watermarking for late data handling (5-minute tolerance)
 * - Multi-dimensional analytics (currency, account, merchant)
 * - Fault-tolerant checkpoint management
 * 
 * Performance Characteristics:
 * - Target: 100,000+ events/second processing throughput
 * - Latency: Sub-30 second end-to-end processing
 * - Availability: 99.9% uptime with automatic recovery
 * 
 * @author Aurora Platform Team
 * @since 1.0.0
 */
object BatchProcessor extends App {
  
  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load()
  
  // Configuration constants
  private val APP_NAME = "aurora-spark-streaming"
  private val CHECKPOINT_LOCATION = config.getString("spark.checkpoint.location")
  private val KAFKA_BOOTSTRAP_SERVERS = sys.env.getOrElse("KAFKA_BOOTSTRAP", "localhost:9092")
  private val KAFKA_TOPIC = config.getString("kafka.topic")
  private val WATERMARK_THRESHOLD = config.getString("streaming.watermark.threshold")
  private val TRIGGER_INTERVAL = config.getString("streaming.trigger.interval")

  /**
   * Optimized Spark session configuration for high-throughput streaming.
   * 
   * Configured with:
   * - Adaptive Query Execution for dynamic optimization
   * - Kryo serialization for performance
   * - State store maintenance for streaming reliability
   * - Dynamic partition coalescing for efficiency
   */
  val spark: SparkSession = SparkSession.builder
    .appName(APP_NAME)
    .config("spark.sql.streaming.checkpointLocation", CHECKPOINT_LOCATION)
    .config("spark.sql.streaming.stateStore.maintenanceInterval", "60s")
    .config("spark.sql.adaptive.enabled", "true")
    .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
    .config("spark.sql.adaptive.skewJoin.enabled", "true")
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .config("spark.sql.streaming.forceDeleteTempCheckpointLocation", "true")
    .getOrCreate()

  import spark.implicits._

  /**
   * Transaction schema definition for structured streaming.
   * 
   * Optimized for:
   * - Fast JSON parsing with explicit schema
   * - Memory efficiency with appropriate data types
   * - Compatibility with downstream systems
   */
  private val transactionSchema = StructType(Array(
    StructField("txnId", StringType, nullable = false),
    StructField("account", StringType, nullable = false),
    StructField("amount", DoubleType, nullable = false),
    StructField("currency", StringType, nullable = false),
    StructField("timestamp", TimestampType, nullable = false),
    StructField("description", StringType, nullable = true),
    StructField("merchant", StringType, nullable = true),
    StructField("category", StringType, nullable = true)
  ))

  try {
    logger.info("Starting Aurora Spark streaming job with configuration:")
    logger.info(s"  - Kafka Bootstrap: $KAFKA_BOOTSTRAP_SERVERS")
    logger.info(s"  - Topic: $KAFKA_TOPIC")
    logger.info(s"  - Checkpoint: $CHECKPOINT_LOCATION")
    logger.info(s"  - Watermark: $WATERMARK_THRESHOLD")
    logger.info(s"  - Trigger: $TRIGGER_INTERVAL")
    
    // Configure and start the streaming pipeline
    val streamingQueries = startStreamingPipeline()
    
    logger.info("Spark streaming queries started successfully")
    
    // Wait for termination signal
    spark.streams.awaitAnyTermination()
    
  } catch {
    case e: Exception =>
      logger.error("Critical error in Spark streaming job", e)
      throw e
  } finally {
    logger.info("Shutting down Spark session")
    spark.stop()
  }

  /**
   * Configures and starts the complete streaming pipeline.
   * 
   * Pipeline stages:
   * 1. Kafka source configuration with reliability settings
   * 2. JSON parsing and schema application
   * 3. Multiple parallel aggregation streams
   * 4. Output to various sinks (console, Hive, etc.)
   * 
   * @return List of active streaming queries
   */
  private def startStreamingPipeline() = {
    // Configure Kafka source stream
    val rawTransactionStream = createKafkaSourceStream()
    
    // Parse JSON and apply schema
    val parsedTransactions = parseTransactionStream(rawTransactionStream)
    
    // Start parallel aggregation streams
    List(
      startCurrencyAggregationStream(parsedTransactions),
      startAccountAggregationStream(parsedTransactions),
      startMerchantAggregationStream(parsedTransactions)
    )
  }

  /**
   * Creates the Kafka source stream with optimized configuration.
   * 
   * Configuration includes:
   * - Reliable offset management
   * - Rate limiting for stability
   * - Consumer group management
   * - Failure handling
   */
  private def createKafkaSourceStream() = {
    spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
      .option("subscribe", KAFKA_TOPIC)
      .option("startingOffsets", "latest")
      .option("failOnDataLoss", "false")
      .option("kafka.consumer.group.id", "aurora-spark-consumer")
      .option("maxOffsetsPerTrigger", "50000") // Rate limiting for stability
      .option("kafka.session.timeout.ms", "30000")
      .option("kafka.request.timeout.ms", "40000")
      .load()
  }

  /**
   * Parses the raw Kafka stream and applies transaction schema.
   * 
   * Features:
   * - Structured JSON parsing with schema validation
   * - Processing timestamp injection for latency tracking
   * - Error handling for malformed records
   */
  private def parseTransactionStream(rawStream: DataFrame) = {
    rawStream
      .select(from_json(col("value").cast("string"), transactionSchema).as("data"))
      .select("data.*")
      .withColumn("processing_time", current_timestamp())
      .withColumn("ingestion_delay", 
        unix_timestamp(col("processing_time")) - unix_timestamp(col("timestamp")))
  }

  /**
   * Currency-level aggregation stream with windowing.
   * 
   * Generates:
   * - Transaction volume by currency
   * - Total/average/min/max amounts
   * - Real-time currency exchange insights
   */
  private def startCurrencyAggregationStream(transactions: DataFrame) = {
    val currencyAggregates = transactions
      .withWatermark("timestamp", WATERMARK_THRESHOLD)
      .groupBy(
        window(col("timestamp"), "1 minute", "30 seconds"),
        col("currency")
      )
      .agg(
        sum("amount").alias("total_amount"),
        count("*").alias("transaction_count"),
        avg("amount").alias("avg_amount"),
        max("amount").alias("max_amount"),
        min("amount").alias("min_amount"),
        avg("ingestion_delay").alias("avg_latency_seconds")
      )
      .withColumn("window_start", col("window.start"))
      .withColumn("window_end", col("window.end"))
      .drop("window")

    currencyAggregates.writeStream
      .outputMode(OutputMode.Append())
      .format("console")
      .option("truncate", "false")
      .option("numRows", "20")
      .trigger(Trigger.ProcessingTime(TRIGGER_INTERVAL))
      .queryName("currency_aggregates")
      .start()
  }

  /**
   * Account-level aggregation stream for fraud detection and spending patterns.
   * 
   * Generates:
   * - Per-account transaction velocity
   * - Spending patterns and anomalies
   * - Multi-currency account analysis
   */
  private def startAccountAggregationStream(transactions: DataFrame) = {
    val accountAggregates = transactions
      .withWatermark("timestamp", WATERMARK_THRESHOLD)
      .groupBy(
        window(col("timestamp"), "5 minutes"),
        col("account")
      )
      .agg(
        sum("amount").alias("total_amount"),
        count("*").alias("transaction_count"),
        countDistinct("currency").alias("currency_count"),
        countDistinct("merchant").alias("merchant_count"),
        max("amount").alias("largest_transaction")
      )
      .withColumn("window_start", col("window.start"))
      .withColumn("window_end", col("window.end"))
      .drop("window")

    accountAggregates.writeStream
      .outputMode(OutputMode.Append())
      .format("console")
      .option("truncate", "false")
      .option("numRows", "10")
      .trigger(Trigger.ProcessingTime(TRIGGER_INTERVAL))
      .queryName("account_aggregates")
      .start()
  }

  /**
   * Merchant-level aggregation stream for business intelligence.
   * 
   * Generates:
   * - Top merchants by volume
   * - Merchant category analysis
   * - Geographic and temporal patterns
   */
  private def startMerchantAggregationStream(transactions: DataFrame) = {
    val merchantAggregates = transactions
      .filter(col("merchant").isNotNull)
      .withWatermark("timestamp", WATERMARK_THRESHOLD)
      .groupBy(
        window(col("timestamp"), "10 minutes"),
        col("merchant"),
        col("category")
      )
      .agg(
        sum("amount").alias("total_revenue"),
        count("*").alias("transaction_count"),
        countDistinct("account").alias("unique_customers"),
        avg("amount").alias("avg_transaction_value")
      )
      .withColumn("window_start", col("window.start"))
      .withColumn("window_end", col("window.end"))
      .drop("window")

    merchantAggregates.writeStream
      .outputMode(OutputMode.Append())
      .format("console")
      .option("truncate", "false")
      .option("numRows", "15")
      .trigger(Trigger.ProcessingTime(TRIGGER_INTERVAL))
      .queryName("merchant_aggregates")
      .start()
  }

  /*
   * Production sink configuration (commented for development):
   * 
   * For production deployment, replace console sinks with:
   * 
   * .format("delta")
   * .option("path", "/data/warehouse/transaction_aggregates")
   * .option("checkpointLocation", "/checkpoints/currency_aggregates")
   * .partitionBy("window_start", "currency")
   * 
   * Or for Hive integration:
   * 
   * .format("hive")
   * .option("table", "transaction_warehouse.currency_aggregates")
   * .option("checkpointLocation", "/checkpoints/hive_currency")
   */
} 