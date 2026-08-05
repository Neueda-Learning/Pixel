package com.pixel.portfolio.repository;

import com.pixel.portfolio.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByExecutedAtAfter(Instant since);   // powers 3M / 6M history

    @Query("select distinct t.symbol from Transaction t order by t.symbol")
    List<String> findDistinctSymbols();
}
