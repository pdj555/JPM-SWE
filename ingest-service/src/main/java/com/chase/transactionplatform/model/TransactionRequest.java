package com.chase.transactionplatform.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Data Transfer Object for incoming transaction requests.
 * 
 * <p>This class serves as the API contract for external clients submitting transactions
 * to the Aurora platform. It includes comprehensive validation rules and follows
 * immutable design principles for thread safety and predictable behavior.
 * 
 * <p><strong>Design Principles:</strong>
 * <ul>
 *   <li>Input validation at the boundary</li>
 *   <li>Clear separation from internal domain models</li>
 *   <li>Defensive programming with null checks</li>
 *   <li>Immutable after creation for thread safety</li>
 * </ul>
 * 
 * <p><strong>Validation Rules:</strong>
 * <ul>
 *   <li>Account ID: 10-20 alphanumeric characters</li>
 *   <li>Amount: Positive decimal value</li>
 *   <li>Currency: ISO 4217 three-letter code</li>
 *   <li>Description: Optional, max 500 characters</li>
 * </ul>
 * 
 * @author Aurora Platform Team
 * @since 1.0.0
 */
public final class TransactionRequest {

    @NotBlank(message = "Account ID cannot be blank")
    @Size(min = 10, max = 20, message = "Account ID must be between 10 and 20 characters")
    private String accountId;

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency cannot be blank")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO 4217 code")
    private String currency;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Merchant ID cannot exceed 100 characters")
    private String merchantId;

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category;

    /**
     * Default constructor for JSON deserialization.
     * <p><strong>Note:</strong> Use the builder pattern or parameterized constructor for programmatic creation.
     */
    public TransactionRequest() {}

    /**
     * Creates a transaction request with required fields.
     * 
     * @param accountId the account identifier (10-20 characters)
     * @param amount the transaction amount (must be positive)
     * @param currency the ISO 4217 currency code
     */
    public TransactionRequest(String accountId, BigDecimal amount, String currency) {
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
    }

    /**
     * Creates a transaction request with all fields.
     * 
     * @param accountId the account identifier
     * @param amount the transaction amount
     * @param currency the ISO 4217 currency code
     * @param description optional transaction description
     * @param merchantId optional merchant identifier
     * @param category optional transaction category
     */
    public TransactionRequest(String accountId, BigDecimal amount, String currency,
                             String description, String merchantId, String category) {
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.merchantId = merchantId;
        this.category = category;
    }

    /**
     * Returns the account identifier for this transaction.
     * @return the account ID (10-20 characters)
     */
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
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
     * Returns the optional merchant identifier.
     * @return the merchant ID or null if not provided
     */
    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
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
        
        TransactionRequest that = (TransactionRequest) obj;
        return Objects.equals(accountId, that.accountId) &&
               Objects.equals(amount, that.amount) &&
               Objects.equals(currency, that.currency) &&
               Objects.equals(description, that.description) &&
               Objects.equals(merchantId, that.merchantId) &&
               Objects.equals(category, that.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, amount, currency, description, merchantId, category);
    }

    @Override
    public String toString() {
        return String.format(
            "TransactionRequest{accountId='%s', amount=%s, currency='%s', " +
            "description='%s', merchantId='%s', category='%s'}",
            accountId, amount, currency, description, merchantId, category
        );
    }
} 