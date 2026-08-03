package com.pixel.portfolio.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ErrorResponse", description = "Standard error response returned by the API")
public class ErrorResponse {

	@Schema(example = "2026-08-03T10:15:30Z")
	private Instant timestamp;

	@Schema(example = "400")
	private int status;

	@Schema(example = "Bad Request")
	private String error;

	@Schema(example = "Validation failed")
	private String message;

	@Schema(example = "/api/portfolios")
	private String path;

	private List<String> details;
}

