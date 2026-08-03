package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.AiChatRequest;
import com.pixel.portfolio.dto.AiChatResponse;
import com.pixel.portfolio.dto.ErrorResponse;
import com.pixel.portfolio.service.GroqService;
import com.pixel.portfolio.service.PortfolioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "AI", description = "Groq-backed AI assistant endpoints")
public class AiController {

	private final GroqService groqService;
	private final PortfolioService portfolioService;

	public AiController(GroqService groqService, PortfolioService portfolioService) {
		this.groqService = groqService;
		this.portfolioService = portfolioService;
	}

	@PostMapping("/ai-chat")
	@Operation(summary = "AI chat", description = "Gets AI answer with optional portfolio context from request")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "AI response", content = @Content(schema = @Schema(implementation = AiChatResponse.class))),
			@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<AiChatResponse> aiChat(@Valid @RequestBody AiChatRequest request) {
		String context = request.getPortfolioId() == null ? "" : portfolioService.buildPortfolioContext(request.getPortfolioId());
		return ResponseEntity.ok(groqService.chat(request.getQuestion(), context));
	}

	@PostMapping("/{portfolioId}/ai-chat")
	@Operation(summary = "AI chat by portfolio", description = "Gets AI answer scoped to a portfolio")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "AI response", content = @Content(schema = @Schema(implementation = AiChatResponse.class))),
			@ApiResponse(responseCode = "404", description = "Portfolio not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))) })
	public ResponseEntity<AiChatResponse> aiChatByPortfolio(@PathVariable Long portfolioId,
			@Valid @RequestBody AiChatRequest request) {
		String context = portfolioService.buildPortfolioContext(portfolioId);
		return ResponseEntity.ok(groqService.chat(request.getQuestion(), context));
	}
}

