package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.NewsItemDto;
import com.pixel.portfolio.dto.ProfileDto;
import com.pixel.portfolio.dto.QuoteDto;
import com.pixel.portfolio.dto.SearchResultDto;

import java.util.List;

/**
 * Live market data (quote, company profile, news, search). Implementations
 * must never let an upstream outage propagate as a 500 — degrade gracefully.
 */
public interface MarketDataService {
    QuoteDto getQuote(String symbol);
    ProfileDto getProfile(String symbol);
    List<NewsItemDto> getNews(String symbol);
    List<SearchResultDto> search(String query);
}

