package com.pixel.portfolio.dto;

import jakarta.validation.constraints.NotNull;
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
@Schema(name = "PortfolioIdRequest", description = "Simple request model containing a portfolio identifier")
public class PortfolioIdRequest {

	@NotNull
	@Positive
	@Schema(example = "1")
	private Long portfolioId;
}

