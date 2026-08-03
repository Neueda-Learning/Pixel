package com.pixel.portfolio.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AiChatResponse", description = "Structured response returned by the Groq assistant")
public class AiChatResponse {

	@Schema(example = "What should I do with my concentrated technology holdings?")
	private String question;

	@Schema(example = "Consider trimming concentration in your largest position and rebalancing into underweighted sectors.")
	private String answer;

	@Schema(example = "llama-3.1-70b-versatile")
	private String model;

	@Schema(example = "2026-08-03T10:15:30Z")
	private Instant timestamp;
}

