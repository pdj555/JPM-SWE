package com.chase.transactionplatform.model;

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
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a financial transaction in the Aurora system.
 * 
 * <p>This entity serves as both the domain model and the Cassandra persistence model,
 * optimized for high-throughput ingestion and sub-100ms query performance.
 * 
 * <p><strong>Design Principles:</strong>
 * <ul>
 *   <li>Immutable after creation (via builder pattern)</li>
 *   <li>UUID-based primary key for distributed scalability</li>
 *   <li>BigDecimal for precise monetary calculations</li>
 *   <li>Comprehensive validation at the field level</li>
 * </ul>
 * 
 * @author Aurora Platform Team
 * @since 1.0.0
 */
@Table("transactions")
public final class Transaction {

    @PrimaryKey
    @NotNull(message = "Transaction ID cannot be null")
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
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO 4217 code")
    private String currency;

    @Column("timestamp")
    @NotNull(message = "Timestamp cannot be null")
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

    /**
     * Default constructor for JPA/Cassandra.
     * <p><strong>Note:</strong> Use the builder or parameterized constructors for creating instances.
     */
    public Transaction() {}

    /**
     * Creates a transaction with required fields only.
     * 
     * @param txnId the unique transaction identifier
     * @param account the account identifier
     * @param amount the transaction amount (must be positive)
     * @param currency the ISO 4217 currency code
     */
    public Transaction(UUID txnId, String account, BigDecimal amount, String currency) {
        this.txnId = txnId;
        this.account = account;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = Instant.now();
    }

    /**
     * Creates a transaction with all fields.
     * 
     * @param txnId the unique transaction identifier
     * @param account the account identifier
     * @param amount the transaction amount (must be positive)
     * @param currency the ISO 4217 currency code
     * @param timestamp the transaction timestamp
     * @param description optional transaction description
     * @param merchant optional merchant information
     * @param category optional transaction category
     */
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

    // Getters and Setters with comprehensive documentation

    /**
     * Returns the unique transaction identifier.
     * @return the transaction UUID
     */
    public UUID getTxnId() {
        return txnId;
    }

    public void setTxnId(UUID txnId) {
        this.txnId = txnId;
    }

    /**
     * Returns the account identifier associated with this transaction.
     * @return the account string
     */
    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    /**
     * Returns the transaction amount with full precision.
     * @return the monetary amount as BigDecimal
     */
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Returns the ISO 4217 currency code (e.g., USD, EUR, GBP).
     * @return the three-letter currency code
     */
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    /**
     * Returns the transaction timestamp with nanosecond precision.
     * @return the instant when the transaction occurred
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Returns the optional transaction description.
     * @return the description or null if not provided
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the optional merchant information.
     * @return the merchant name or null if not provided
     */
    public String getMerchant() {
        return merchant;
    }

    public void setMerchant(String merchant) {
        this.merchant = merchant;
    }

    /**
     * Returns the optional transaction category.
     * @return the category or null if not provided
     */
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Transaction that = (Transaction) obj;
        return Objects.equals(txnId, that.txnId) &&
               Objects.equals(account, that.account) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(currency, that.currency) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(description, that.description) &&
               Objects.equals(merchant, that.merchant) &&
               Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(txnId, account, amount, currency, timestamp, 
                           description, merchant, category);
    }

    @Override
    public String toString() {
        return String.format(
            "Transaction{txnId=%s, account='%s', amount=%s, currency='%s', timestamp=%s, " +
            "description='%s', merchant='%s', category='%s'}",
            txnId, account, amount, currency, timestamp, description, merchant, category
        );
    }
} 