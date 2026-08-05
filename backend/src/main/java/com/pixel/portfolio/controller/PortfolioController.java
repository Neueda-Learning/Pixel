package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.HoldingDto;
import com.pixel.portfolio.dto.PerformancePointDto;
import com.pixel.portfolio.dto.PortfolioSummaryDto;
import com.pixel.portfolio.service.PortfolioService;
import com.pixel.portfolio.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "Portfolio", description = "Holdings, totals, and performance derived from the transaction ledger")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final TransactionService transactionService;

    public PortfolioController(PortfolioService portfolioService, TransactionService transactionService) {
        this.portfolioService = portfolioService;
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "Get current holdings")
    public List<HoldingDto> getHoldings() {
        return portfolioService.getHoldings();
    }

    @GetMapping("/summary")
    @Operation(summary = "Get portfolio summary")
    public PortfolioSummaryDto getSummary() {
        return portfolioService.getSummary();
    }

    @GetMapping("/performance")
    @Operation(summary = "Get portfolio value over time")
    public List<PerformancePointDto> getPerformance(
            @Parameter(description = "1M, 3M, 6M, 1Y, or ALL") @RequestParam(defaultValue = "6M") String period) {
        return portfolioService.getPerformance(period);
    }

    @GetMapping("/export")
    @Operation(summary = "Export portfolio as CSV", description = "Downloads a CSV with all holdings and transactions.")
    public ResponseEntity<String> exportPortfolio() {
        List<HoldingDto> holdings = portfolioService.getHoldings();
        var transactions = transactionService.list("ALL");
        var summary = portfolioService.getSummary();

        StringBuilder csv = new StringBuilder();
        csv.append("# Pixel Portfolio Export — ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        // Summary section
        csv.append("## PORTFOLIO SUMMARY\n");
        csv.append("Total Value,Total Cost,Total Gain/Loss,Gain/Loss %,Holdings Count\n");
        csv.append(String.format("%s,%s,%s,%s,%d\n\n",
                summary.getTotalValue(), summary.getTotalCost(),
                summary.getTotalGainLoss(), summary.getTotalGainLossPct(),
                summary.getHoldingsCount()));

        // Holdings section
        csv.append("## HOLDINGS\n");
        csv.append("Symbol,Name,Asset Type,Quantity,Avg Cost,Current Price,Market Value,Gain/Loss,Gain/Loss %\n");
        for (HoldingDto h : holdings) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    h.getSymbol(), escape(h.getName()), h.getAssetType(),
                    h.getQuantity(), h.getAvgCost(), h.getCurrentPrice(),
                    h.getMarketValue(), h.getGainLoss(), h.getGainLossPct()));
        }
        csv.append("\n");

        // Transactions section
        csv.append("## TRANSACTIONS\n");
        csv.append("ID,Symbol,Type,Quantity,Price,Fees,Executed At,Notes\n");
        for (var t : transactions) {
            csv.append(String.format("%d,%s,%s,%s,%s,%s,%s,%s\n",
                    t.getId(), t.getSymbol(), t.getTxType(),
                    t.getQuantity(), t.getPrice(), t.getFees(),
                    t.getExecutedAt(), escape(t.getNotes() != null ? t.getNotes() : "")));
        }

        String filename = "pixel-portfolio-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    private String escape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
