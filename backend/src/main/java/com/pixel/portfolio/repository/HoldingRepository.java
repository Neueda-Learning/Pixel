package com.pixel.portfolio.repository;

import java.util.List;
import java.util.Optional;

import com.pixel.portfolio.entity.Holding;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

	List<Holding> findByPortfolioIdOrderBySymbolAsc(Long portfolioId);

	Optional<Holding> findByPortfolioIdAndSymbolIgnoreCase(Long portfolioId, String symbol);

	boolean existsByPortfolioId(Long portfolioId);

	void deleteByPortfolioIdAndSymbolIgnoreCase(Long portfolioId, String symbol);
}

