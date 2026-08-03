package com.pixel.portfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1000)
public class MockMarketPriceProvider implements MarketPriceProvider {

	@Override
	public boolean supports(String symbol) {
		return symbol != null && !symbol.isBlank();
	}

	@Override
	public BigDecimal getPrice(String symbol) {
		String normalized = symbol.trim().toUpperCase();
		int hash = Math.abs(normalized.hashCode());
		double base = 25.0 + (hash % 5000) / 100.0;
		return BigDecimal.valueOf(base).setScale(4, RoundingMode.HALF_UP);
	}

	@Override
	public String getName() {
		return "mock-provider";
	}
}

