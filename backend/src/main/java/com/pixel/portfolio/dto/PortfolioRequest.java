package com.pixel.portfolio.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PortfolioRequest", description = "Create a portfolio with an initial cash balance")
public class PortfolioRequest {

	@NotBlank
	@Schema(example = "Growth Portfolio")
	private String name;

	@NotNull
	@PositiveOrZero
	@Schema(example = "10000.00")
	private BigDecimal amount;
}

