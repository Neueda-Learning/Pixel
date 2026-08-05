package com.pixel.portfolio.repository;

import com.pixel.portfolio.model.PriceHistory;
import com.pixel.portfolio.model.PriceHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, PriceHistoryId> {
    List<PriceHistory> findBySymbolOrderByTradeDateAsc(String symbol);

    List<PriceHistory> findBySymbolAndTradeDateGreaterThanEqualOrderByTradeDateAsc(String symbol, LocalDate from);

    long countBySymbol(String symbol);

    Optional<PriceHistory> findTopBySymbolOrderByTradeDateDesc(String symbol);

    @Query("select distinct p.symbol from PriceHistory p order by p.symbol")
    List<String> findDistinctSymbols();
}
