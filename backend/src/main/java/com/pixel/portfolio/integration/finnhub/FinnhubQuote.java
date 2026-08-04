package com.pixel.portfolio.integration.finnhub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/** Raw Finnhub GET /quote response — field names mirror the API exactly. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubQuote(
        BigDecimal c,  // current
        BigDecimal d,  // change
        BigDecimal dp, // percent change
        BigDecimal h,  // high
        BigDecimal l,  // low
        BigDecimal o,  // open
        BigDecimal pc  // previous close
) {}

