package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.HoldingDto;
import com.pixel.portfolio.dto.PerformancePointDto;
import com.pixel.portfolio.dto.PortfolioSummaryDto;
import com.pixel.portfolio.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "Portfolio", description = "Holdings, totals, and performance derived from the transaction ledger")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    @Operation(summary = "Get current holdings", description = "Derives holdings (qty, avg cost, current price, market value, gain/loss) from all transactions using the average-cost method.")
    public List<HoldingDto> getHoldings() {
        return portfolioService.getHoldings();
    }

    @GetMapping("/summary")
    @Operation(summary = "Get portfolio summary", description = "Total value, total cost, total gain/loss, and allocation breakdown by asset type.")
    public PortfolioSummaryDto getSummary() {
        return portfolioService.getSummary();
    }

    @GetMapping("/performance")
    @Operation(summary = "Get portfolio value over time", description = "Historical portfolio value, computed by replaying holdings against price_history — for the performance chart.")
    public List<PerformancePointDto> getPerformance(
            @Parameter(description = "1M, 3M, 6M, 1Y, or ALL") @RequestParam(defaultValue = "6M") String period) {
        return portfolioService.getPerformance(period);
    }
}
