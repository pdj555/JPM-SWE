package com.chase.aurora.controller;

import com.chase.aurora.model.Transaction;
import com.chase.aurora.model.TransactionRequest;
import com.chase.aurora.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.annotation.Counted;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/transactions")
public class TransactionController {

  private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
  private final TransactionService service;

  public TransactionController(TransactionService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Timed(value = "aurora.http.request.duration", description = "Time taken for HTTP requests")
  @Counted(value = "aurora.http.requests", description = "Number of HTTP requests")
  public ResponseEntity<Map<String, String>> post(@RequestBody @Valid TransactionRequest request) {
    // Convert DTO to entity with auto-generated fields
    Transaction txn = mapToTransaction(request);
    
    log.info("Received transaction request: txnId={}, account={}, amount={} {}", 
        txn.getTxnId(), txn.getAccount(), txn.getAmount(), txn.getCurrency());
    
    try {
      service.ingest(txn);
      return ResponseEntity.accepted()
          .body(Map.of("status", "accepted", "txnId", txn.getTxnId().toString()));
    } catch (Exception e) {
      log.error("Failed to process transaction [{}]", txn.getTxnId(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(Map.of("status", "error", "message", "Transaction processing failed"));
    }
  }

  @GetMapping("/{txnId}")
  @Timed(value = "aurora.http.request.duration", description = "Time taken for HTTP requests")
  public ResponseEntity<Transaction> get(@PathVariable("txnId") String txnId) {
    log.debug("Looking up transaction: {}", txnId);
    
    Transaction transaction = service.findById(UUID.fromString(txnId));
    if (transaction != null) {
      return ResponseEntity.ok(transaction);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/lookup")
  @Timed(value = "aurora.http.request.duration", description = "Time taken for HTTP requests")
  public ResponseEntity<Transaction> lookup(@RequestParam("id") String txnId) {
    log.debug("Looking up transaction via query param: {}", txnId);
    
    try {
      Transaction transaction = service.findById(UUID.fromString(txnId));
      if (transaction != null) {
        return ResponseEntity.ok(transaction);
      } else {
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      log.error("Error looking up transaction [{}]", txnId, e);
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    return ResponseEntity.ok(Map.of("status", "UP", "service", "aurora-ingest"));
  }

  /**
   * Maps TransactionRequest DTO to Transaction entity with auto-generated fields
   */
  private Transaction mapToTransaction(TransactionRequest request) {
    Transaction transaction = new Transaction();
    transaction.setTxnId(UUID.randomUUID());  // Auto-generate UUID
    transaction.setTimestamp(Instant.now());  // Auto-generate timestamp
    transaction.setAccount(request.getAccountId());  // Map accountId -> account
    transaction.setAmount(request.getAmount());
    transaction.setCurrency(request.getCurrency());
    transaction.setDescription(request.getDescription());
    transaction.setMerchant(request.getMerchantId());  // Map merchantId -> merchant
    transaction.setCategory(request.getCategory());
    return transaction;
  }
} 