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
@Schema(name = "HoldingWeightResponse", description = "Holding weight entry used in diversity analysis")
public class HoldingWeightResponse {

	@Schema(example = "AAPL")
	private String symbol;

	@Schema(example = "0.2850")
	private BigDecimal weight;

	@Schema(example = "15250.0000")
	private BigDecimal marketValue;
}

