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
@Schema(name = "HoldingResponse", description = "Holding details enriched with live market information")
public class HoldingResponse {

	@Schema(example = "AAPL")
	private String symbol;

	@Schema(example = "12.500000")
	private BigDecimal quantity;

	@Schema(example = "150.2500")
	private BigDecimal averagePrice;

	@Schema(example = "152.1000")
	private BigDecimal marketPrice;

	@Schema(example = "1901.2500")
	private BigDecimal marketValue;

	@Schema(example = "23.1250")
	private BigDecimal pnl;
}

