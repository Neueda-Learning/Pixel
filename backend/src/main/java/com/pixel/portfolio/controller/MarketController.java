package com.pixel.portfolio.controller;

import com.pixel.portfolio.dto.NewsItemDto;
import com.pixel.portfolio.dto.ProfileDto;
import com.pixel.portfolio.dto.QuoteDto;
import com.pixel.portfolio.dto.SearchResultDto;
import com.pixel.portfolio.service.MarketDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/market")
@Tag(name = "Market", description = "Finnhub-backed live quote/profile/news proxy, cached server-side with DB fallback")
public class MarketController {

    private final MarketDataService marketDataService;

    public MarketController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }

    @GetMapping("/quote/{symbol}")
    @Operation(summary = "Get live quote", description = "Live quote from Finnhub (cached 30s); falls back to the latest price_history close if Finnhub is unavailable.")
    public QuoteDto getQuote(@PathVariable String symbol) {
        return marketDataService.getQuote(symbol);
    }

    @GetMapping("/profile/{symbol}")
    @Operation(summary = "Get company profile", description = "Company name/logo/exchange/currency from Finnhub (cached 24h); falls back to reference data.")
    public ProfileDto getProfile(@PathVariable String symbol) {
        return marketDataService.getProfile(symbol);
    }

    @GetMapping("/news/{symbol}")
    @Operation(summary = "Get company news", description = "Recent company news from Finnhub (cached 10m). Returns an empty list if unavailable.")
    public List<NewsItemDto> getNews(@PathVariable String symbol) {
        return marketDataService.getNews(symbol);
    }

    @GetMapping("/search")
    @Operation(summary = "Search symbols", description = "Symbol/company search via Finnhub, falling back to local instrument reference data if unavailable.")
    public List<SearchResultDto> search(@RequestParam String q) {
        return marketDataService.search(q);
    }
}

