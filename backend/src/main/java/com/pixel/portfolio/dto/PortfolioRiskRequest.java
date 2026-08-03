package com.pixel.portfolio.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PortfolioRiskRequest", description = "Request body for risk analysis")
public class PortfolioRiskRequest {

	@Positive
	@Schema(example = "1", description = "Optional portfolio identifier when calling the global endpoint")
	private Long portfolioId;

	@Positive
	@Schema(example = "0.35", description = "Maximum allowed weight for a single holding")
	private BigDecimal maxSingleHoldingWeight;

	@Positive
	@Schema(example = "0.15", description = "Stress percentage used by what-if risk projections")
	private BigDecimal stressPercent;
}
