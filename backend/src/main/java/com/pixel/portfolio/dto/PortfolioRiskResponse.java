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
@Schema(name = "PortfolioRiskResponse", description = "Risk analysis result for a portfolio")
public class PortfolioRiskResponse {

	@Schema(example = "1")
	private Long portfolioId;

	@Schema(example = "64")
	private Integer riskScore;

	@Schema(example = "MODERATE")
	private String riskLevel;

	@Schema(example = "0.4200")
	private BigDecimal maxSingleHoldingWeight;

	@Schema(example = "0.1800")
	private BigDecimal stressPercent;

	@Schema(example = "Reduce concentration in the largest holding to lower risk.")
	private String message;
}

