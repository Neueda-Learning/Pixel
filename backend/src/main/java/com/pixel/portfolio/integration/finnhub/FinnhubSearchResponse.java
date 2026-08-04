package com.pixel.portfolio.integration.finnhub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubSearchResponse(int count, List<FinnhubSearchResult> result) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinnhubSearchResult(String description, String displaySymbol, String symbol, String type) {}
}

