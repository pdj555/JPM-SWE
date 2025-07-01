package com.chase.aurora.model;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("transactions")
public class Transaction {

    @PrimaryKey
    @NotNull
    private UUID txnId;

    @Column("account")
    @NotBlank(message = "Account cannot be blank")
    @Size(min = 10, max = 20, message = "Account must be between 10 and 20 characters")
    private String account;

    @Column("amount")
    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @Column("currency")
    @NotBlank(message = "Currency cannot be blank")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency;

    @Column("timestamp")
    @NotNull
    private Instant timestamp;

    @Column("description")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Column("merchant")
    @Size(max = 100, message = "Merchant name cannot exceed 100 characters")
    private String merchant;

    @Column("category")
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;

    // Default constructor
    public Transaction() {}

    // Constructor with required fields
    public Transaction(UUID txnId, String account, BigDecimal amount, String currency) {
        this.txnId = txnId;
        this.account = account;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = Instant.now();
    }

    // All-args constructor
    public Transaction(UUID txnId, String account, BigDecimal amount, String currency, 
                      Instant timestamp, String description, String merchant, String category) {
        this.txnId = txnId;
        this.account = account;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = timestamp;
        this.description = description;
        this.merchant = merchant;
        this.category = category;
    }

    // Getters and Setters
    public UUID getTxnId() {
        return txnId;
    }

    public void setTxnId(UUID txnId) {
        this.txnId = txnId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
} 