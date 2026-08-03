package com.pixel.portfolio.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "WhatIfResponse", description = "Projected portfolio impact from a hypothetical trade")
public class WhatIfResponse {

	@Schema(example = "1")
	private Long portfolioId;

	@Schema(example = "BUY AAPL 5 @ 150.2500")
	private String scenario;

	@Schema(example = "12500.0000")
	private BigDecimal beforeCashBalance;

	@Schema(example = "11248.7500")
	private BigDecimal afterCashBalance;

	@Schema(example = "21250.0000")
	private BigDecimal beforeMarketValue;

	@Schema(example = "21250.0000")
	private BigDecimal afterMarketValue;

	@Schema(example = "33750.0000")
	private BigDecimal projectedTotalValue;

	@Schema(example = "0.0000")
	private BigDecimal delta;

	@Schema(example = "The trade is affordable and would not change total value at execution price.")
	private String message;
}

