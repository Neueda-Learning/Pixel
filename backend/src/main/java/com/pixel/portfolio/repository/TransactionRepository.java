package com.pixel.portfolio.repository;

import com.pixel.portfolio.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByExecutedAtAfter(Instant since);   // powers 3M / 6M history
}
