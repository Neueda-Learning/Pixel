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
@Schema(name = "PortfolioHealthResponse", description = "Health score and status for a portfolio")
public class PortfolioHealthResponse {

	@Schema(example = "1")
	private Long portfolioId;

	@Schema(example = "82")
	private Integer score;

	@Schema(example = "HEALTHY")
	private String status;

	@Schema(example = "0.2500")
	private BigDecimal cashRatio;

	@Schema(example = "0.4200")
	private BigDecimal concentrationRisk;

	@Schema(example = "Cash ratio is adequate and holdings are diversified.")
	private String message;
}

