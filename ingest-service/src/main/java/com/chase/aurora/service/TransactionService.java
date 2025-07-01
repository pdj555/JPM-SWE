package com.chase.aurora.service;

import com.chase.aurora.model.Transaction;
import com.chase.aurora.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class TransactionService {

  private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
  private final TransactionRepository repository;
  private final KafkaTemplate<String, Transaction> kafka;
  private final MeterRegistry meterRegistry;

  public TransactionService(TransactionRepository repository, 
                          KafkaTemplate<String, Transaction> kafka,
                          MeterRegistry meterRegistry) {
    this.repository = repository;
    this.kafka = kafka;
    this.meterRegistry = meterRegistry;
  }

  @Transactional
  public void ingest(Transaction txn) {
    Timer.Sample sample = Timer.start(meterRegistry);
    
    try {
      // Set timestamp if not provided
      if (txn.getTimestamp() == null) {
        txn.setTimestamp(Instant.now());
      }
      
      // Dual-write pattern: persist to Cassandra first
      repository.save(txn);
      log.debug("Transaction [{}] saved to Cassandra", txn.getTxnId());
      
      // Then publish to Kafka for downstream consumers
      CompletableFuture<SendResult<String, Transaction>> future = 
          kafka.send("txn.v1", txn.getTxnId().toString(), txn);
      
      future.whenComplete((result, ex) -> {
        if (ex != null) {
          log.error("Failed to publish transaction [{}] to Kafka", txn.getTxnId(), ex);
          meterRegistry.counter("aurora.kafka.send.errors").increment();
        } else {
          log.debug("Transaction [{}] published to Kafka topic: {}, partition: {}", 
              txn.getTxnId(), result.getRecordMetadata().topic(), 
              result.getRecordMetadata().partition());
          meterRegistry.counter("aurora.kafka.send.success").increment();
        }
      });
      
      log.info("Transaction [{}] ingested successfully - Account: {}, Amount: {} {}", 
          txn.getTxnId(), txn.getAccount(), txn.getAmount(), txn.getCurrency());
      
      // Update metrics
      meterRegistry.counter("aurora.transactions.ingested").increment();
      meterRegistry.summary("aurora.transaction.amount").record(txn.getAmount().doubleValue());
      
    } catch (Exception e) {
      log.error("Failed to ingest transaction [{}]", txn.getTxnId(), e);
      meterRegistry.counter("aurora.transactions.failed").increment();
      throw e;
    } finally {
      sample.stop(Timer.builder("aurora.transaction.ingest.duration")
          .description("Time taken to ingest a transaction")
          .register(meterRegistry));
    }
  }
  
  public Transaction findById(UUID txnId) {
    return repository.findById(txnId).orElse(null);
  }
} 