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
@Schema(name = "PortfolioHealthRequest", description = "Request body for portfolio health scoring")
public class PortfolioHealthRequest {

	@Positive
	@Schema(example = "1", description = "Optional portfolio identifier when calling the global endpoint")
	private Long portfolioId;

	@Positive
	@Schema(example = "0.10", description = "Minimum acceptable cash ratio before the portfolio is flagged")
	private BigDecimal minimumCashRatio;
}
