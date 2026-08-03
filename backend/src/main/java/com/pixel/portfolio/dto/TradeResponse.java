package com.pixel.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "TradeResponse", description = "Trade details returned after execution or during history lookup")
public class TradeResponse {

	@Schema(example = "100")
	private Long id;

	@Schema(example = "1")
	private Long portfolioId;

	@Schema(example = "AAPL")
	private String symbol;

	@Schema(example = "10.000000")
	private BigDecimal quantity;

	@Schema(example = "150.2500")
	private BigDecimal price;

	@Schema(example = "BUY")
	private String type;

	@Schema(example = "2026-08-03T10:15:30")
	private LocalDateTime createdAt;

	@Schema(example = "1502.5000")
	private BigDecimal tradeValue;
}

