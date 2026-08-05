package com.pixel.portfolio.integration.finnhub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubProfile(String name, String logo, String exchange, String currency, String ticker) {}

