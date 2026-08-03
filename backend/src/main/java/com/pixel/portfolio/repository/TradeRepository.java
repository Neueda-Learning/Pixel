package com.pixel.portfolio.repository;

import java.util.List;

import com.pixel.portfolio.entity.Trade;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {

	List<Trade> findByPortfolioIdOrderByCreatedAtDesc(Long portfolioId);
}

