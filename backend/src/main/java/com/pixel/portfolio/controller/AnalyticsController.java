package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.ErrorResponse;
import com.pixel.portfolio.dto.PortfolioDiversityResponse;
import com.pixel.portfolio.dto.PortfolioHealthRequest;
import com.pixel.portfolio.dto.PortfolioHealthResponse;
import com.pixel.portfolio.dto.PortfolioIdRequest;
import com.pixel.portfolio.dto.PortfolioPnLResponse;
import com.pixel.portfolio.dto.PortfolioRiskRequest;
import com.pixel.portfolio.dto.PortfolioRiskResponse;
import com.pixel.portfolio.dto.WhatIfRequest;
import com.pixel.portfolio.dto.WhatIfResponse;
import com.pixel.portfolio.service.AnalyticsService;
import com.pixel.portfolio.util.ResponseUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "Analytics", description = "Portfolio analytics endpoints")
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	public AnalyticsController(AnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	@GetMapping("/{id}/pnl")
	@Operation(summary = "Calculate portfolio PnL", description = "Calculates total and per-holding PnL")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "PnL calculated", content = @Content(schema = @Schema(implementation = PortfolioPnLResponse.class))),
			@ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<PortfolioPnLResponse> pnl(@PathVariable Long id) {
		return ResponseEntity.ok(analyticsService.getPnL(id));
	}

	@PostMapping("/health")
	@Operation(summary = "Portfolio health (global endpoint)", description = "Calculates health using request portfolioId")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Health calculated", content = @Content(schema = @Schema(implementation = PortfolioHealthResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<PortfolioHealthResponse> health(@Valid @RequestBody PortfolioHealthRequest request) {
		return ResponseEntity.ok(analyticsService.getHealth(request));
	}

	@PostMapping("/{portfolioId}/health")
	@Operation(summary = "Portfolio health (path endpoint)", description = "Calculates health using path portfolioId")
	@ApiResponse(responseCode = "200", description = "Health calculated")
	public ResponseEntity<PortfolioHealthResponse> healthById(@PathVariable Long portfolioId,
			@RequestBody(required = false) PortfolioHealthRequest request) {
		if (request != null && request.getMinimumCashRatio() != null) {
			PortfolioHealthRequest merged = PortfolioHealthRequest.builder()
					.portfolioId(portfolioId)
					.minimumCashRatio(request.getMinimumCashRatio())
					.build();
			return ResponseEntity.ok(analyticsService.getHealth(merged));
		}
		return ResponseEntity.ok(analyticsService.getHealth(portfolioId));
	}

	@PostMapping("/risk")
	@Operation(summary = "Portfolio risk (global endpoint)", description = "Calculates risk using request portfolioId")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Risk calculated", content = @Content(schema = @Schema(implementation = PortfolioRiskResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<PortfolioRiskResponse> risk(@Valid @RequestBody PortfolioRiskRequest request) {
		return ResponseEntity.ok(analyticsService.getRisk(request));
	}

	@PostMapping("/{portfolioId}/risk")
	@Operation(summary = "Portfolio risk (path endpoint)", description = "Calculates risk using path portfolioId")
	@ApiResponse(responseCode = "200", description = "Risk calculated")
	public ResponseEntity<PortfolioRiskResponse> riskById(@PathVariable Long portfolioId,
			@RequestBody(required = false) PortfolioRiskRequest request) {
		PortfolioRiskRequest merged = PortfolioRiskRequest.builder()
				.portfolioId(portfolioId)
				.maxSingleHoldingWeight(request == null ? null : request.getMaxSingleHoldingWeight())
				.stressPercent(request == null ? null : request.getStressPercent())
				.build();
		return ResponseEntity.ok(analyticsService.getRisk(merged));
	}

	@PostMapping("/diversity")
	@Operation(summary = "Portfolio diversity", description = "Calculates portfolio concentration and diversity metrics")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Diversity calculated", content = @Content(schema = @Schema(implementation = PortfolioDiversityResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<PortfolioDiversityResponse> diversity(@Valid @RequestBody PortfolioIdRequest request) {
		return ResponseEntity.ok(analyticsService.getDiversity(request.getPortfolioId()));
	}

	@PostMapping("/what-if")
	@Operation(summary = "What-if analysis", description = "Simulates the impact of a hypothetical trade")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Scenario calculated", content = @Content(schema = @Schema(implementation = WhatIfResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<WhatIfResponse> whatIf(@Valid @RequestBody WhatIfRequest request) {
		return ResponseEntity.ok(analyticsService.whatIf(request));
	}

	@GetMapping(value = "/diversity/csv/{portfolioId}", produces = "text/csv")
	@Operation(summary = "Export diversity CSV", description = "Exports portfolio diversity metrics as CSV")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "CSV generated", content = @Content(mediaType = "text/csv")),
			@ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<byte[]> diversityCsv(@PathVariable Long portfolioId) {
		String csv = analyticsService.diversityCsv(portfolioId);
		return ResponseUtil.csvDownload("portfolio-" + portfolioId + "-diversity.csv", csv);
	}
}

