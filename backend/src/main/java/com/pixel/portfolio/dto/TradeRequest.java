package com.pixel.portfolio.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
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
@Schema(name = "TradeRequest", description = "Query parameters for buy and sell trade operations")
public class TradeRequest {

	@NotNull
	@Positive
	@Schema(example = "1")
	private Long portfolioId;

	@NotBlank
	@Schema(example = "AAPL")
	private String symbol;

	@NotNull
	@Positive
	@Schema(example = "10")
	private BigDecimal quantity;

	@Positive
	@Schema(example = "150.25", description = "Optional execution price. If omitted, the live market price is used.")
	private BigDecimal price;
}

