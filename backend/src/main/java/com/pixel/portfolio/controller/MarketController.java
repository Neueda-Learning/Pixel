package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.ErrorResponse;
import com.pixel.portfolio.dto.MarketPriceResponse;
import com.pixel.portfolio.service.MarketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
@Tag(name = "Market", description = "Market data endpoints")
public class MarketController {

	private final MarketService marketService;

	public MarketController(MarketService marketService) {
		this.marketService = marketService;
	}

	@GetMapping("/live")
	@Operation(summary = "Get live market price", description = "Returns current market price from configured provider")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Price fetched", content = @Content(schema = @Schema(implementation = MarketPriceResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid symbol", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<MarketPriceResponse> live(@RequestParam String symbol) {
		return ResponseEntity.ok(marketService.getLivePrice(symbol));
	}
}

