package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.RiskDto;
import com.pixel.portfolio.service.RiskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
@Tag(name = "Risk", description = "Rule-based risk metrics and BUY/HOLD/AVOID recommendation")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/{symbol}")
    @Operation(summary = "Get risk metrics and recommendation",
            description = "Volatility, Sharpe ratio, max drawdown, beta (vs SPY), SMA 50/200 trend, and RSI(14), "
                    + "computed from price_history, plus a transparent rule-based BUY/HOLD/AVOID recommendation. "
                    + "Educational only, not financial advice.")
    public RiskDto getRisk(@PathVariable String symbol) {
        return riskService.getRisk(symbol);
    }
}

