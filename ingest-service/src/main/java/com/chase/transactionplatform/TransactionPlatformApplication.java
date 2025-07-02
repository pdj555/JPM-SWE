package com.chase.transactionplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * High-Performance Transaction Processing Platform - Main Application Entry Point
 * 
 * <p>Enterprise-grade transaction processing service engineered for 25,000+ TPS
 * with sub-100ms p99 latency. Built with JPMorgan Chase engineering excellence standards.
 * 
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Dual-write pattern (Cassandra + Kafka)</li>
 *   <li>Project Loom virtual threads for concurrency</li>
 *   <li>Comprehensive observability and metrics</li>
 *   <li>Cloud-native deployment with Kubernetes</li>
 * </ul>
 * 
 * <p><strong>Architecture:</strong>
 * Follows microservices patterns with event-driven architecture,
 * CQRS principles, and eventual consistency for downstream systems.
 * 
 * @author JPMorgan Chase Platform Engineering
 * @since 1.0.0
 * @version 1.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableTransactionManagement
public class TransactionPlatformApplication {

    /**
     * Application entry point.
     * 
     * <p>Initializes the Spring Boot application context with optimized
     * configuration for high-throughput transaction processing.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(TransactionPlatformApplication.class, args);
    }
} 