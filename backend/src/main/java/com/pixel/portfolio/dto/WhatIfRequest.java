package com.pixel.portfolio.dto;

import java.math.BigDecimal;

import com.pixel.portfolio.entity.TradeType;

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
@Schema(name = "WhatIfRequest", description = "Request body for simulating a buy or sell scenario")
public class WhatIfRequest {

	@NotNull
	@Positive
	@Schema(example = "1")
	private Long portfolioId;

	@NotBlank
	@Schema(example = "AAPL")
	private String symbol;

	@NotNull
	@Positive
	@Schema(example = "5")
	private BigDecimal quantity;

	@NotNull
	@Positive
	@Schema(example = "150.2500")
	private BigDecimal price;

	@NotNull
	@Schema(example = "BUY")
	private TradeType type;
}

