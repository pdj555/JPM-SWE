package com.chase.transactionplatform;

import com.chase.transactionplatform.controller.TransactionController;
import com.chase.transactionplatform.model.Transaction;
import com.chase.transactionplatform.model.TransactionRequest;
import com.chase.transactionplatform.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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
 * Unit tests for TransactionController.
 * 
 * <p>These tests focus on testing the controller logic without loading Spring context.
 * 
 * @author Aurora Platform Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Controller Unit Tests")
public class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private TransactionController transactionController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
        objectMapper = new ObjectMapper();
    }

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
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(transactionService).ingest(any(Transaction.class));
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
                    .andExpect(status().isInternalServerError());
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
            mockMvc.perform(get("/v1/transactions/{txnId}", txnId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(transactionService).findById(txnId);
        }

        @Test
        @DisplayName("Should return 404 when transaction not found")
        public void shouldReturn404WhenTransactionNotFound() throws Exception {
            // Given
            UUID txnId = UUID.randomUUID();
            when(transactionService.findById(txnId)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/v1/transactions/{txnId}", txnId))
                    .andExpect(status().isNotFound());

            verify(transactionService).findById(txnId);
        }
    }

    @Nested
    @DisplayName("Health Check")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return service health status")
        public void shouldReturnServiceHealthStatus() throws Exception {
            // When & Then
            mockMvc.perform(get("/v1/transactions/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
} 