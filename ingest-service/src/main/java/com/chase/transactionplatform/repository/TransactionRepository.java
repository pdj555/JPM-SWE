package com.chase.transactionplatform.repository;

import com.chase.transactionplatform.model.Transaction;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Repository
public interface TransactionRepository extends CassandraRepository<Transaction, UUID> {
  
  // Find transactions by account
  @Query("SELECT * FROM transactions WHERE account = ?0 LIMIT 100")
  List<Transaction> findByAccount(String account);
  
  // Find transactions by currency
  List<Transaction> findByCurrency(String currency);
  
  // Custom query for transactions within time range
  @Query("SELECT * FROM transactions WHERE timestamp >= ?0 AND timestamp <= ?1 ALLOW FILTERING")
  List<Transaction> findByTimestampRange(Instant startTime, Instant endTime);

  @Query("SELECT * FROM transactions WHERE account = ?0 AND timestamp >= ?1 LIMIT 1000")
  List<Transaction> findByAccountAndTimestampAfter(String account, java.time.Instant timestamp);
} 