package com.pixel.portfolio.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.pixel.portfolio.dto.AiChatResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class GroqService {

	private final RestTemplate restTemplate;
	private final String apiKey;
	private final String model;
	private final String baseUrl;

	public GroqService(RestTemplate restTemplate,
			@Value("${groq.api-key:}") String apiKey,
			@Value("${groq.model:llama-3.1-70b-versatile}") String model,
			@Value("${groq.base-url:https://api.groq.com/openai/v1/chat/completions}") String baseUrl) {
		this.restTemplate = restTemplate;
		this.apiKey = apiKey;
		this.model = model;
		this.baseUrl = baseUrl;
	}

	public AiChatResponse chat(String question, String context) {
		if (!StringUtils.hasText(apiKey)) {
			return fallback(question, context, "Groq API key is not configured. Provide GROQ_API_KEY to enable live AI responses.");
		}

		try {
			Map<String, Object> payload = Map.of(
					"model", model,
					"messages", List.of(
						Map.of("role", "system", "content", systemPrompt()),
						Map.of("role", "user", "content", prompt(question, context))),
					"temperature", 0.2);

			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(apiKey.trim());
			headers.setContentType(MediaType.APPLICATION_JSON);

			ResponseEntity<JsonNode> response = restTemplate.exchange(baseUrl, HttpMethod.POST, new HttpEntity<>(payload, headers), JsonNode.class);
			String answer = extractAnswer(response.getBody());
			return AiChatResponse.builder().question(question).answer(answer).model(model).timestamp(Instant.now()).build();
		} catch (RestClientException ex) {
			return fallback(question, context, "Groq request failed: " + ex.getMessage());
		}
	}

	private AiChatResponse fallback(String question, String context, String answer) {
		return AiChatResponse.builder()
				.question(question)
				.answer(context == null || context.isBlank() ? answer : answer + " Context: " + context)
				.model(model)
				.timestamp(Instant.now())
				.build();
	}

	private String extractAnswer(JsonNode body) {
		if (body == null) {
			return "No response body returned by Groq.";
		}
		JsonNode choices = body.path("choices");
		if (!choices.isArray() || choices.isEmpty()) {
			return body.toString();
		}
		JsonNode message = choices.get(0).path("message");
		String content = message.path("content").asText(null);
		return content == null || content.isBlank() ? body.toString() : content;
	}

	private String systemPrompt() {
		return "You are Pixel's portfolio investment assistant. Give concise, structured guidance and avoid inventing market data.";
	}

	private String prompt(String question, String context) {
		if (!StringUtils.hasText(context)) {
			return question;
		}
		return question + "\n\nPortfolio context:\n" + context;
	}
}

