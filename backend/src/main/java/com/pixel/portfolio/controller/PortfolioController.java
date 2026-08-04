package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.HoldingDto;
import com.pixel.portfolio.dto.PortfolioSummaryDto;
import com.pixel.portfolio.service.PortfolioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public List<HoldingDto> getHoldings() {
        return portfolioService.getHoldings();
    }

    @GetMapping("/summary")
    public PortfolioSummaryDto getSummary() {
        return portfolioService.getSummary();
    }
}
