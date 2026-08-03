package com.pixel.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "MarketPriceResponse", description = "Live or provider-backed market price for a symbol")
public class MarketPriceResponse {

	@Schema(example = "AAPL")
	private String symbol;

	@Schema(example = "152.1000")
	private BigDecimal price;

	@Schema(example = "mock-provider")
	private String provider;

	@Schema(example = "2026-08-03T10:15:30Z")
	private Instant timestamp;
}

