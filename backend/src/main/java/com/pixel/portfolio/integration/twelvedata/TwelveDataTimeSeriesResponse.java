package com.pixel.portfolio.integration.twelvedata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response shape for GET /time_series. On success, {@code status} is "ok" and {@code values} is
 * populated. On failure (bad symbol, rate limit, bad key), {@code status} is "error" and
 * {@code code}/{@code message} describe why — {@code values} is absent in that case.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TwelveDataTimeSeriesResponse(
        String status,
        List<TwelveDataValue> values,
        Integer code,
        String message
) {
}
