package com.pixel.portfolio.integration.finnhub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubNewsItem(
        long id,
        String headline,
        String summary,
        String url,
        String source,
        String image,
        long datetime // unix seconds
) {}

