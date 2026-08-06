package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.ChatRequestDto;
import com.pixel.portfolio.dto.ChatResponseDto;
import com.pixel.portfolio.service.ChatBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chatbot", description = "Rule-based portfolio assistant — no external AI service, answers derived from real portfolio/risk data")
public class ChatController {

    private final ChatBotService chatBotService;

    public ChatController(ChatBotService chatBotService) {
        this.chatBotService = chatBotService;
    }

    @PostMapping
    @Operation(summary = "Ask the portfolio assistant a question",
            description = "Matches the message against a fixed set of keyword/threshold heuristics "
                    + "(best/worst performer, allocation & rebalancing, risk, performance, holdings) "
                    + "and answers using live PortfolioService/RiskService data.")
    public ChatResponseDto chat(@Valid @RequestBody ChatRequestDto request) {
        return chatBotService.respond(request.getMessage());
    }
}
