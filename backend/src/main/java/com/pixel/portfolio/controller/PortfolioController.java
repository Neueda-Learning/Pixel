package com.pixel.portfolio.controller;

import java.math.BigDecimal;
import java.util.List;

import com.pixel.portfolio.dto.ErrorResponse;
import com.pixel.portfolio.dto.HoldingResponse;
import com.pixel.portfolio.dto.PortfolioRequest;
import com.pixel.portfolio.dto.PortfolioResponse;
import com.pixel.portfolio.dto.TradeResponse;
import com.pixel.portfolio.service.PortfolioService;
import com.pixel.portfolio.util.ResponseUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolios")
@Validated
@Tag(name = "Portfolio", description = "Portfolio management operations")
public class PortfolioController {

	private final PortfolioService portfolioService;

	public PortfolioController(PortfolioService portfolioService) {
		this.portfolioService = portfolioService;
	}

	@PostMapping
	@Operation(summary = "Create portfolio", description = "Creates a portfolio with a name and initial cash amount")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Portfolio created", content = @Content(schema = @Schema(implementation = PortfolioResponse.class))),
			@ApiResponse(responseCode = "400", description = "Validation or business rule error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<PortfolioResponse> createPortfolio(
			@Parameter(description = "Portfolio name", example = "Growth Portfolio") @RequestParam String name,
			@Parameter(description = "Initial cash", example = "10000") @PositiveOrZero @RequestParam BigDecimal amount) {
		PortfolioRequest request = PortfolioRequest.builder().name(name).amount(amount).build();
		return ResponseEntity.ok(portfolioService.createPortfolio(request));
	}

	@GetMapping
	@Operation(summary = "List portfolios", description = "Returns all portfolios")
	@ApiResponse(responseCode = "200", description = "Portfolios fetched")
	public ResponseEntity<List<PortfolioResponse>> getPortfolios() {
		return ResponseEntity.ok(portfolioService.getAllPortfolios());
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete portfolio", description = "Deletes a portfolio if it has no holdings")
	@ApiResponses({
			@ApiResponse(responseCode = "204", description = "Portfolio deleted"),
			@ApiResponse(responseCode = "400", description = "Cannot delete portfolio with holdings", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<Void> deletePortfolio(@PathVariable Long id) {
		portfolioService.deletePortfolio(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/add-funds")
	@Operation(summary = "Add funds", description = "Adds funds to an existing portfolio")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Funds added", content = @Content(schema = @Schema(implementation = PortfolioResponse.class))),
			@ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<PortfolioResponse> addFunds(@PathVariable Long id, @Positive @RequestParam BigDecimal amount) {
		return ResponseEntity.ok(portfolioService.addFunds(id, amount));
	}

	@PostMapping("/{id}/withdraw-funds")
	@Operation(summary = "Withdraw funds", description = "Withdraws funds from an existing portfolio if balance is sufficient")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Funds withdrawn", content = @Content(schema = @Schema(implementation = PortfolioResponse.class))),
			@ApiResponse(responseCode = "400", description = "Insufficient balance", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
			@ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<PortfolioResponse> withdrawFunds(@PathVariable Long id, @Positive @RequestParam BigDecimal amount) {
		return ResponseEntity.ok(portfolioService.withdrawFunds(id, amount));
	}

	@GetMapping("/{id}/holdings")
	@Operation(summary = "Get holdings", description = "Returns holdings for a portfolio")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Holdings fetched"),
			@ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<List<HoldingResponse>> getHoldings(@PathVariable Long id) {
		return ResponseEntity.ok(portfolioService.getHoldings(id));
	}

	@GetMapping("/{portfolioId}/trades")
	@Operation(summary = "Get trade history by portfolio", description = "Returns trade history for the given portfolio")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Trades fetched"),
			@ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<List<TradeResponse>> getTrades(@PathVariable Long portfolioId) {
		return ResponseEntity.ok(portfolioService.getTrades(portfolioId));
	}

	@GetMapping(value = "/statement/csv/{portfolioId}", produces = "text/csv")
	@Operation(summary = "Export portfolio statement CSV", description = "Exports the portfolio statement as CSV")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "CSV generated", content = @Content(mediaType = "text/csv")),
			@ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<byte[]> exportStatement(@PathVariable Long portfolioId) {
		String csv = portfolioService.exportStatementCsv(portfolioId);
		return ResponseUtil.csvDownload("portfolio-" + portfolioId + "-statement.csv", csv);
	}
}
