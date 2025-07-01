package com.chase.aurora.spark

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.types._
import com.typesafe.config.ConfigFactory
import org.slf4j.{Logger, LoggerFactory}

object BatchProcessor extends App {
  
  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val config = ConfigFactory.load()
  
  // Spark session with optimizations for high throughput
  val spark = SparkSession.builder
    .appName("aurora-spark-job")
    .config("spark.sql.streaming.checkpointLocation", "/tmp/aurora-checkpoint")
    .config("spark.sql.streaming.stateStore.maintenanceInterval", "60s")
    .config("spark.sql.adaptive.enabled", "true")
    .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
    .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .getOrCreate()

  import spark.implicits._

  // Transaction schema for JSON parsing
  val transactionSchema = StructType(Array(
    StructField("txnId", StringType, nullable = false),
    StructField("account", StringType, nullable = false),
    StructField("amount", DoubleType, nullable = false),
    StructField("currency", StringType, nullable = false),
    StructField("timestamp", TimestampType, nullable = false)
  ))

  try {
    logger.info("Starting Aurora Spark streaming job...")
    
    // Read from Kafka topic
    val kafkaBootstrap = sys.env.getOrElse("KAFKA_BOOTSTRAP", "localhost:9092")
    val txnStream = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaBootstrap)
      .option("subscribe", "txn.v1")
      .option("startingOffsets", "latest")
      .option("failOnDataLoss", "false")
      .option("kafka.consumer.group.id", "aurora-spark-consumer")
      .option("maxOffsetsPerTrigger", "10000") // Rate limiting
      .load()

    // Parse JSON and extract transaction data
    val parsedTransactions = txnStream
      .select(from_json(col("value").cast("string"), transactionSchema).as("data"))
      .select("data.*")
      .withColumn("processing_time", current_timestamp())

    // Real-time aggregations with windowing
    val windowedAggregates = parsedTransactions
      .withWatermark("timestamp", "5 minutes")
      .groupBy(
        window(col("timestamp"), "1 minute", "30 seconds"),
        col("currency")
      )
      .agg(
        sum("amount").alias("total_amount"),
        count("*").alias("transaction_count"),
        avg("amount").alias("avg_amount"),
        max("amount").alias("max_amount"),
        min("amount").alias("min_amount")
      )
      .withColumn("window_start", col("window.start"))
      .withColumn("window_end", col("window.end"))
      .drop("window")

    // Account-level aggregations
    val accountAggregates = parsedTransactions
      .withWatermark("timestamp", "5 minutes")
      .groupBy(
        window(col("timestamp"), "5 minutes"),
        col("account")
      )
      .agg(
        sum("amount").alias("total_amount"),
        count("*").alias("transaction_count"),
        countDistinct("currency").alias("currency_count")
      )
      .withColumn("window_start", col("window.start"))
      .withColumn("window_end", col("window.end"))
      .drop("window")

    // Write aggregates to console (replace with actual sink in production)
    val currencyQuery = windowedAggregates.writeStream
      .outputMode(OutputMode.Append())
      .format("console")
      .option("truncate", "false")
      .trigger(Trigger.ProcessingTime("30 seconds"))
      .queryName("currency_aggregates")
      .start()

    val accountQuery = accountAggregates.writeStream
      .outputMode(OutputMode.Append())
      .format("console")
      .option("truncate", "false")
      .trigger(Trigger.ProcessingTime("30 seconds"))
      .queryName("account_aggregates")
      .start()

    // In production, you would write to Hive/Delta Lake:
    /*
    val hiveQuery = windowedAggregates.writeStream
      .outputMode(OutputMode.Append())
      .format("delta")
      .option("path", "/data/warehouse/transaction_aggregates")
      .option("checkpointLocation", "/tmp/delta-checkpoint")
      .trigger(Trigger.ProcessingTime("1 minute"))
      .start()
    */

    logger.info("Spark streaming queries started successfully")
    
    // Wait for all streams to finish
    spark.streams.awaitAnyTermination()
    
  } catch {
    case e: Exception =>
      logger.error("Error in Spark streaming job", e)
      throw e
  } finally {
    logger.info("Shutting down Spark session")
    spark.stop()
  }
} 