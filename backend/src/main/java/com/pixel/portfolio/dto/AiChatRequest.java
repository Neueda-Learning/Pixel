package com.pixel.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
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
@Schema(name = "AiChatRequest", description = "Question payload for the Groq-backed portfolio assistant")
public class AiChatRequest {

	@Positive
	@Schema(example = "1", description = "Optional portfolio context identifier")
	private Long portfolioId;

	@NotBlank
	@Schema(example = "What should I do with my concentrated technology holdings?")
	private String question;
}

