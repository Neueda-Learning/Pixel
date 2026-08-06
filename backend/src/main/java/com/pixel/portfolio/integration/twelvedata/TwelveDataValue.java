package com.pixel.portfolio.integration.twelvedata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One daily OHLCV row from Twelve Data's time_series endpoint. All fields arrive as strings. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TwelveDataValue(
        String datetime,
        String open,
        String high,
        String low,
        String close,
        String volume
) {
}
