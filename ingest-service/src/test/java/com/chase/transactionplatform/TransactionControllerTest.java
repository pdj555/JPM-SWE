package com.chase.transactionplatform;

import com.chase.transactionplatform.config.TestConfig;
import com.chase.transactionplatform.controller.TransactionController;
import com.chase.transactionplatform.model.Transaction;
import com.chase.transactionplatform.model.TransactionRequest;
import com.chase.transactionplatform.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for TransactionController.
 * 
 * <p>This test class validates the REST API behavior with Apple-grade test coverage,
 * including happy paths, edge cases, error scenarios, and performance characteristics.
 * 
 * <p><strong>Test Categories:</strong>
 * <ul>
 *   <li>Transaction Creation - Valid and invalid requests</li>
 *   <li>Transaction Retrieval - Found and not found scenarios</li>
 *   <li>Error Handling - Validation and runtime errors</li>
 *   <li>Health Checks - Service availability</li>
 * </ul>
 * 
 * @author Aurora Platform Team
 * @since 1.0.0
 */
@WebMvcTest(TransactionController.class)
@DisplayName("Transaction Controller Tests")
@Import(TestConfig.class)
@ActiveProfiles("test")
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Transaction Creation")
    class TransactionCreationTests {

        @Test
        @DisplayName("Should accept valid transaction request")
        public void shouldAcceptValidTransactionRequest() throws Exception {
            // Given
            TransactionRequest request = new TransactionRequest(
                "1234567890",
                new BigDecimal("100.00"),
                "USD",
                "Test transaction",
                "Test Merchant",
                "Groceries"
            );

            doNothing().when(transactionService).ingest(any(Transaction.class));

            // When & Then
            mockMvc.perform(post("/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("accepted"))
                    .andExpect(jsonPath("$.txnId").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.message").value("Transaction submitted for processing"));

            verify(transactionService).ingest(any(Transaction.class));
        }

        @Test
        @DisplayName("Should reject transaction with invalid account")
        public void shouldRejectTransactionWithInvalidAccount() throws Exception {
            // Given
            TransactionRequest request = new TransactionRequest(
                "123", // Too short
                new BigDecimal("100.00"),
                "USD"
            );

            // When & Then
            mockMvc.perform(post("/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

            verify(transactionService, never()).ingest(any(Transaction.class));
        }

        @Test
        @DisplayName("Should reject transaction with negative amount")
        public void shouldRejectTransactionWithNegativeAmount() throws Exception {
            // Given
            TransactionRequest request = new TransactionRequest(
                "1234567890",
                new BigDecimal("-100.00"), // Negative amount
                "USD"
            );

            // When & Then
            mockMvc.perform(post("/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

            verify(transactionService, never()).ingest(any(Transaction.class));
        }

        @Test
        @DisplayName("Should reject transaction with invalid currency")
        public void shouldRejectTransactionWithInvalidCurrency() throws Exception {
            // Given
            TransactionRequest request = new TransactionRequest(
                "1234567890",
                new BigDecimal("100.00"),
                "INVALID" // Invalid currency code
            );

            // When & Then
            mockMvc.perform(post("/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));

            verify(transactionService, never()).ingest(any(Transaction.class));
        }

        @Test
        @DisplayName("Should handle service exception gracefully")
        public void shouldHandleServiceExceptionGracefully() throws Exception {
            // Given
            TransactionRequest request = new TransactionRequest(
                "1234567890",
                new BigDecimal("100.00"),
                "USD"
            );

            doThrow(new RuntimeException("Service unavailable")).when(transactionService).ingest(any(Transaction.class));

            // When & Then
            mockMvc.perform(post("/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"));
        }
    }

    @Nested
    @DisplayName("Transaction Retrieval")
    class TransactionRetrievalTests {

        @Test
        @DisplayName("Should return transaction when found")
        public void shouldReturnTransactionWhenFound() throws Exception {
            // Given
            UUID txnId = UUID.randomUUID();
            Transaction transaction = new Transaction(
                txnId,
                "1234567890",
                new BigDecimal("100.00"),
                "USD",
                Instant.now(),
                "Test transaction",
                "Test Merchant",
                "Groceries"
            );

            when(transactionService.findById(txnId)).thenReturn(Optional.of(transaction));

            // When & Then
            mockMvc.perform(get("/v1/transactions/" + txnId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.txnId").value(txnId.toString()))
                    .andExpect(jsonPath("$.account").value("1234567890"))
                    .andExpect(jsonPath("$.amount").value(100.00))
                    .andExpect(jsonPath("$.currency").value("USD"));

            verify(transactionService).findById(txnId);
        }

        @Test
        @DisplayName("Should return 404 when transaction not found")
        public void shouldReturn404WhenTransactionNotFound() throws Exception {
            // Given
            UUID txnId = UUID.randomUUID();
            when(transactionService.findById(txnId)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/v1/transactions/" + txnId.toString()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.txnId").value(txnId.toString()));

            verify(transactionService).findById(txnId);
        }

        @Test
        @DisplayName("Should return 400 for invalid UUID format")
        public void shouldReturn400ForInvalidUUIDFormat() throws Exception {
            // Given
            String invalidUuid = "invalid-uuid";

            // When & Then
            mockMvc.perform(get("/v1/transactions/" + invalidUuid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error").value("INVALID_FORMAT"))
                    .andExpect(jsonPath("$.txnId").value(invalidUuid));

            verify(transactionService, never()).findById(any(UUID.class));
        }

        @Test
        @DisplayName("Should support legacy lookup endpoint")
        public void shouldSupportLegacyLookupEndpoint() throws Exception {
            // Given
            UUID txnId = UUID.randomUUID();
            Transaction transaction = new Transaction(
                txnId,
                "1234567890",
                new BigDecimal("100.00"),
                "USD"
            );

            when(transactionService.findById(txnId)).thenReturn(Optional.of(transaction));

            // When & Then
            mockMvc.perform(get("/v1/transactions/lookup").param("id", txnId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.txnId").value(txnId.toString()));

            verify(transactionService).findById(txnId);
        }
    }

    @Nested
    @DisplayName("Health Check")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return service health status")
        public void shouldReturnServiceHealthStatus() throws Exception {
            mockMvc.perform(get("/v1/transactions/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("aurora-ingest"))
                    .andExpect(jsonPath("$.version").value("1.0.0"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }
} 