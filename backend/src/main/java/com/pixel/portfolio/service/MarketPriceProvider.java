package com.pixel.portfolio.service;

import java.math.BigDecimal;

public interface MarketPriceProvider {

	boolean supports(String symbol);

	BigDecimal getPrice(String symbol);

	String getName();
}

