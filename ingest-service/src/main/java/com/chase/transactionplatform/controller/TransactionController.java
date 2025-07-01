package com.chase.transactionplatform.controller;

import com.chase.transactionplatform.model.Transaction;
import com.chase.transactionplatform.model.TransactionRequest;
import com.chase.transactionplatform.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.annotation.Counted;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for high-throughput transaction ingestion and retrieval.
 * 
 * <p>This controller provides a clean, RESTful API for external clients to submit
 * and query financial transactions. It follows Apple-grade principles of simplicity,
 * reliability, and performance with comprehensive error handling and observability.
 * 
 * <p><strong>API Design Principles:</strong>
 * <ul>
 *   <li>RESTful resource-oriented URLs</li>
 *   <li>HTTP status codes that reflect actual state</li>
 *   <li>Comprehensive input validation</li>
 *   <li>Structured error responses</li>
 *   <li>Async processing with immediate acknowledgment</li>
 * </ul>
 * 
 * <p><strong>Performance Characteristics:</strong>
 * <ul>
 *   <li>Target: 25,000 requests/second sustained</li>
 *   <li>Latency: Sub-50ms p99 response time</li>
 *   <li>Validation: < 1ms per request</li>
 * </ul>
 * 
 * @author Aurora Platform Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/transactions")
@Validated
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;

    /**
     * Constructs a new TransactionController with required dependencies.
     * 
     * @param transactionService the service for transaction operations
     */
    public TransactionController(@NonNull TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Submits a new transaction for processing.
     * 
     * <p>This endpoint accepts transaction data and immediately returns an acknowledgment
     * while processing continues asynchronously. The dual-write pattern ensures data
     * consistency across storage systems.
     * 
     * <p><strong>Request Flow:</strong>
     * <ol>
     *   <li>Validates request payload</li>
     *   <li>Converts DTO to domain model</li>
     *   <li>Submits for async processing</li>
     *   <li>Returns immediate acknowledgment</li>
     * </ol>
     * 
     * @param request the transaction request payload (validated)
     * @return HTTP 202 with transaction ID if accepted, HTTP 400 if validation fails
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Timed(value = "aurora.http.request.duration", description = "HTTP request processing time")
    @Counted(value = "aurora.http.requests", description = "Total HTTP requests")
    public ResponseEntity<Map<String, Object>> createTransaction(
            @RequestBody @Valid @NonNull TransactionRequest request) {
        
        log.info("Received transaction request for account: {}, amount: {} {}", 
            request.getAccountId(), request.getAmount(), request.getCurrency());
        
        try {
            // Convert DTO to domain model with system-generated fields
            Transaction transaction = mapToTransaction(request);
            
            // Submit for async processing
            transactionService.ingest(transaction);
            
            // Return immediate acknowledgment
            Map<String, Object> response = Map.of(
                "status", "accepted",
                "txnId", transaction.getTxnId().toString(),
                "timestamp", transaction.getTimestamp().toString(),
                "message", "Transaction submitted for processing"
            );
            
            log.debug("Transaction [{}] accepted for processing", transaction.getTxnId());
            return ResponseEntity.accepted().body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid transaction request: {}", e.getMessage());
            Map<String, Object> errorResponse = Map.of(
                "status", "error",
                "error", "VALIDATION_FAILED",
                "message", e.getMessage(),
                "timestamp", Instant.now().toString()
            );
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            log.error("Unexpected error processing transaction request: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = Map.of(
                "status", "error",
                "error", "INTERNAL_SERVER_ERROR",
                "message", "Transaction processing temporarily unavailable",
                "timestamp", Instant.now().toString()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Retrieves a transaction by its unique identifier.
     * 
     * <p>This endpoint provides fast lookups of previously ingested transactions
     * with sub-10ms response times from the Cassandra cluster.
     * 
     * @param txnId the transaction UUID as a path parameter
     * @return HTTP 200 with transaction data if found, HTTP 404 if not found
     */
    @GetMapping("/{txnId}")
    @Timed(value = "aurora.http.request.duration", description = "HTTP request processing time")
    @Counted(value = "aurora.http.requests", description = "Total HTTP requests")
    public ResponseEntity<Object> getTransaction(@PathVariable("txnId") @NonNull String txnId) {
        log.debug("Looking up transaction: {}", txnId);
        
        try {
            UUID transactionId = UUID.fromString(txnId);
            Optional<Transaction> transaction = transactionService.findById(transactionId);
            
            if (transaction.isPresent()) {
                log.debug("Transaction [{}] found", txnId);
                return ResponseEntity.ok(transaction.get());
            } else {
                log.debug("Transaction [{}] not found", txnId);
                Map<String, Object> errorResponse = Map.of(
                    "status", "error",
                    "error", "NOT_FOUND",
                    "message", "Transaction not found",
                    "txnId", txnId,
                    "timestamp", Instant.now().toString()
                );
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid transaction ID format: {}", txnId);
            Map<String, Object> errorResponse = Map.of(
                "status", "error",
                "error", "INVALID_FORMAT",
                "message", "Transaction ID must be a valid UUID",
                "txnId", txnId,
                "timestamp", Instant.now().toString()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Alternative transaction lookup via query parameter.
     * 
     * <p>This endpoint provides the same functionality as the path-based lookup
     * but accepts the transaction ID as a query parameter for client flexibility.
     * 
     * @param txnId the transaction UUID as a query parameter
     * @return HTTP 200 with transaction data if found, HTTP 404 if not found
     * @deprecated Use GET /{txnId} instead for better RESTful design
     */
    @GetMapping("/lookup")
    @Timed(value = "aurora.http.request.duration", description = "HTTP request processing time")
    @Deprecated
    public ResponseEntity<Object> lookupTransaction(@RequestParam("id") @NonNull String txnId) {
        log.debug("Looking up transaction via query param: {}", txnId);
        return getTransaction(txnId); // Delegate to primary lookup method
    }

    /**
     * Health check endpoint for load balancer and monitoring systems.
     * 
     * <p>This endpoint provides a fast health status without hitting downstream
     * dependencies, ensuring sub-millisecond response times.
     * 
     * @return HTTP 200 with service status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> healthStatus = Map.of(
            "status", "UP",
            "service", "aurora-ingest",
            "version", "1.0.0",
            "timestamp", Instant.now().toString()
        );
        return ResponseEntity.ok(healthStatus);
    }

    /**
     * Maps a TransactionRequest DTO to a Transaction domain entity.
     * 
     * <p>This method handles the conversion from the external API representation
     * to the internal domain model, generating system-controlled fields and
     * applying business rules.
     * 
     * @param request the validated transaction request
     * @return a new Transaction entity ready for processing
     */
    private Transaction mapToTransaction(@NonNull TransactionRequest request) {
        Transaction transaction = new Transaction();
        
        // System-generated fields
        transaction.setTxnId(UUID.randomUUID());
        transaction.setTimestamp(Instant.now());
        
        // Client-provided fields
        transaction.setAccount(request.getAccountId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency().toUpperCase()); // Normalize to uppercase
        transaction.setDescription(request.getDescription());
        transaction.setMerchant(request.getMerchantId());
        transaction.setCategory(request.getCategory());
        
        return transaction;
    }
} 