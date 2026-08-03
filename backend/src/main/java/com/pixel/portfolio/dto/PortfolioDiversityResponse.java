package com.pixel.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PortfolioDiversityResponse", description = "Diversity metrics and holding weights for a portfolio")
public class PortfolioDiversityResponse {

	@Schema(example = "1")
	private Long portfolioId;

	@Schema(example = "Growth Portfolio")
	private String portfolioName;

	@Schema(example = "0.2350")
	private BigDecimal herfindahlIndex;

	@Schema(example = "76.5000")
	private BigDecimal diversityScore;

	@Schema(example = "0.4200")
	private BigDecimal topHoldingWeight;

	@Schema(example = "MODERATE")
	private String concentrationLevel;

	private List<HoldingWeightResponse> holdings;
}

