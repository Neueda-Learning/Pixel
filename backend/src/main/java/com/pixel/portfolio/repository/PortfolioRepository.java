package com.pixel.portfolio.repository;

import java.util.Optional;

import com.pixel.portfolio.entity.Portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

	Optional<Portfolio> findByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCase(String name);
}

