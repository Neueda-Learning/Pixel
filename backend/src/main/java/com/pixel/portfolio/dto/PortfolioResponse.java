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
@Schema(name = "PortfolioResponse", description = "Portfolio summary returned by the API")
public class PortfolioResponse {

	@Schema(example = "1")
	private Long id;

	@Schema(example = "Growth Portfolio")
	private String name;

	@Schema(example = "12500.0000")
	private BigDecimal cashBalance;

	@Schema(example = "4")
	private Integer holdingsCount;

	@Schema(example = "17")
	private Integer tradesCount;
}

