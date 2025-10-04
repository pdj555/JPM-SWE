package com.chase.transactionplatform.config;

import com.chase.transactionplatform.repository.TransactionRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

@TestConfiguration
@Profile("test")
public class TestConfig {

    @Bean
    @Primary
    public TransactionRepository mockTransactionRepository() {
        return mock(TransactionRepository.class);
    }

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public KafkaTemplate<String, Object> mockKafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
} 