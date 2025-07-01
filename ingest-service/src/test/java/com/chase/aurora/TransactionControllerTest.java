package com.chase.aurora;

import com.chase.aurora.controller.TransactionController;
import com.chase.aurora.model.Transaction;
import com.chase.aurora.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
public class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateTransaction() throws Exception {
        Transaction transaction = new Transaction(
            UUID.randomUUID(),
            "1234567890",
            new BigDecimal("100.00"),
            "USD",
            Instant.now(),
            "Test transaction",
            "Test Merchant",
            "Groceries"
        );

        doNothing().when(transactionService).ingest(any(Transaction.class));

        mockMvc.perform(post("/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.txnId").exists());
    }

    @Test
    public void testCreateTransactionValidationError() throws Exception {
        Transaction invalidTransaction = new Transaction(
            UUID.randomUUID(),
            "123", // Invalid account (too short)
            new BigDecimal("100.00"),
            "USD"
        );

        mockMvc.perform(post("/v1/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidTransaction)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetTransaction() throws Exception {
        UUID txnId = UUID.randomUUID();
        Transaction transaction = new Transaction(
            txnId,
            "1234567890",
            new BigDecimal("100.00"),
            "USD"
        );

        when(transactionService.findById(txnId)).thenReturn(transaction);

        mockMvc.perform(get("/v1/transactions/" + txnId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.txnId").value(txnId.toString()))
                .andExpect(jsonPath("$.account").value("1234567890"))
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    public void testGetTransactionNotFound() throws Exception {
        UUID txnId = UUID.randomUUID();
        when(transactionService.findById(txnId)).thenReturn(null);

        mockMvc.perform(get("/v1/transactions/" + txnId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/v1/transactions/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("aurora-ingest"));
    }
} 