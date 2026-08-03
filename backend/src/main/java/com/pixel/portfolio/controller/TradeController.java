package com.pixel.portfolio.controller;

import java.math.BigDecimal;
import java.util.List;

import com.pixel.portfolio.dto.ErrorResponse;
import com.pixel.portfolio.dto.TradeRequest;
import com.pixel.portfolio.dto.TradeResponse;
import com.pixel.portfolio.service.TradeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
@Validated
@Tag(name = "Trade", description = "Trade execution and history endpoints")
public class TradeController {

	private final TradeService tradeService;

	public TradeController(TradeService tradeService) {
		this.tradeService = tradeService;
	}

	@PostMapping("/buy")
	@Operation(summary = "Buy trade", description = "Executes a BUY trade and updates holdings/cash")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Buy executed", content = @Content(schema = @Schema(implementation = TradeResponse.class))),
			@ApiResponse(responseCode = "400", description = "Validation or business error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<TradeResponse> buy(
			@Parameter(example = "1") @Positive @RequestParam Long portfolioId,
			@Parameter(example = "AAPL") @RequestParam String symbol,
			@Parameter(example = "10") @Positive @RequestParam BigDecimal quantity,
			@Parameter(example = "150.25", description = "Optional. If omitted, market price is used") @RequestParam(required = false) BigDecimal price) {
		TradeRequest request = TradeRequest.builder()
				.portfolioId(portfolioId)
				.symbol(symbol)
				.quantity(quantity)
				.price(price)
				.build();
		return ResponseEntity.ok(tradeService.buy(request));
	}

	@PostMapping("/sell")
	@Operation(summary = "Sell trade", description = "Executes a SELL trade and updates holdings/cash")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Sell executed", content = @Content(schema = @Schema(implementation = TradeResponse.class))),
			@ApiResponse(responseCode = "400", description = "Validation or business error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<TradeResponse> sell(
			@Parameter(example = "1") @Positive @RequestParam Long portfolioId,
			@Parameter(example = "AAPL") @RequestParam String symbol,
			@Parameter(example = "5") @Positive @RequestParam BigDecimal quantity,
			@Parameter(example = "151.10", description = "Optional. If omitted, market price is used") @RequestParam(required = false) BigDecimal price) {
		TradeRequest request = TradeRequest.builder()
				.portfolioId(portfolioId)
				.symbol(symbol)
				.quantity(quantity)
				.price(price)
				.build();
		return ResponseEntity.ok(tradeService.sell(request));
	}

	@GetMapping("/portfolio/{portfolioId}")
	@Operation(summary = "Get trades by portfolio", description = "Returns all trades for the given portfolio")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Trades fetched"),
			@ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<List<TradeResponse>> getTradesByPortfolio(@PathVariable Long portfolioId) {
		return ResponseEntity.ok(tradeService.getTradesByPortfolio(portfolioId));
	}
}

