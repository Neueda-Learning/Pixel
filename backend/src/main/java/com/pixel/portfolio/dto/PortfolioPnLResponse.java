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
@Schema(name = "PortfolioPnLResponse", description = "Portfolio profit and loss breakdown")
public class PortfolioPnLResponse {

	@Schema(example = "1")
	private Long portfolioId;

	@Schema(example = "Growth Portfolio")
	private String portfolioName;

	@Schema(example = "12500.0000")
	private BigDecimal cashBalance;

	@Schema(example = "20000.0000")
	private BigDecimal costBasis;

	@Schema(example = "21250.0000")
	private BigDecimal currentMarketValue;

	@Schema(example = "33750.0000")
	private BigDecimal totalPortfolioValue;

	@Schema(example = "1250.0000")
	private BigDecimal totalPnl;

	@Schema(example = "6.2500")
	private BigDecimal pnlPercentage;

	private List<HoldingResponse> holdings;
}

