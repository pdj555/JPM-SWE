package com.chase.transactionplatform.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.validation.FieldError;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for the Aurora Transaction Platform.
 * 
 * <p>This component provides centralized, consistent error handling across
 * all REST endpoints with Apple-grade user experience principles. It ensures
 * that all errors are properly logged, metrics are updated, and clients
 * receive structured, actionable error responses.
 * 
 * <p><strong>Error Response Structure:</strong>
 * <ul>
 *   <li>Consistent JSON structure across all error types</li>
 *   <li>Human-readable error messages</li>
 *   <li>Unique error codes for programmatic handling</li>
 *   <li>Timestamp and request context</li>
 *   <li>Validation details when applicable</li>
 * </ul>
 * 
 * <p><strong>Security Considerations:</strong>
 * <ul>
 *   <li>No internal implementation details exposed</li>
 *   <li>Sanitized error messages for external clients</li>
 *   <li>Comprehensive logging for internal debugging</li>
 * </ul>
 * 
 * @author Aurora Platform Team
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Autowired
    private MeterRegistry meterRegistry;

    /**
     * Handles validation errors from request payload validation.
     * 
     * <p>This handler processes Bean Validation failures and returns
     * detailed field-level error information to help clients correct
     * their requests.
     * 
     * @param ex the validation exception
     * @param request the web request context
     * @return HTTP 400 with detailed validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        log.warn("Validation failed for request: {}", request.getDescription(false));
        meterRegistry.counter("aurora.errors.validation").increment();
        
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (existing, replacement) -> existing // Keep first error if multiple
            ));
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("error", "VALIDATION_FAILED");
        errorResponse.put("message", "Request validation failed");
        errorResponse.put("details", fieldErrors);
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", request.getDescription(false));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles illegal argument exceptions from business logic validation.
     * 
     * @param ex the illegal argument exception
     * @param request the web request context
     * @return HTTP 400 with error details
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        
        log.warn("Invalid argument: {} for request: {}", ex.getMessage(), request.getDescription(false));
        meterRegistry.counter("aurora.errors.invalid_argument").increment();
        
        Map<String, Object> errorResponse = Map.of(
            "status", "error",
            "error", "INVALID_ARGUMENT",
            "message", ex.getMessage(),
            "timestamp", Instant.now().toString(),
            "path", request.getDescription(false)
        );
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles unexpected runtime exceptions with proper error concealment.
     * 
     * <p>This handler ensures that internal implementation details are not
     * exposed to clients while providing comprehensive logging for debugging.
     * 
     * @param ex the runtime exception
     * @param request the web request context
     * @return HTTP 500 with generic error message
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        log.error("Unexpected runtime error for request {}: {}", 
            request.getDescription(false), ex.getMessage(), ex);
        meterRegistry.counter("aurora.errors.runtime").increment();
        
        Map<String, Object> errorResponse = Map.of(
            "status", "error",
            "error", "INTERNAL_SERVER_ERROR",
            "message", "An unexpected error occurred. Please try again later.",
            "timestamp", Instant.now().toString(),
            "path", request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles all other unexpected exceptions as a safety net.
     * 
     * @param ex the exception
     * @param request the web request context
     * @return HTTP 500 with generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected exception for request {}: {}", 
            request.getDescription(false), ex.getMessage(), ex);
        meterRegistry.counter("aurora.errors.unexpected").increment();
        
        Map<String, Object> errorResponse = Map.of(
            "status", "error",
            "error", "UNEXPECTED_ERROR",
            "message", "An unexpected error occurred. Please contact support if this persists.",
            "timestamp", Instant.now().toString(),
            "path", request.getDescription(false)
        );
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
} 