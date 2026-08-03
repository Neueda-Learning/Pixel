package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.MarketPriceResponse;

public interface MarketService {

	MarketPriceResponse getLivePrice(String symbol);
}

