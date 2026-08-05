package com.pixel.portfolio.service;

import com.pixel.portfolio.integration.twelvedata.TwelveDataTimeSeriesResponse;
import com.pixel.portfolio.integration.twelvedata.TwelveDataValue;
import com.pixel.portfolio.model.PriceHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fetches real daily OHLCV history from the Twelve Data API (https://twelvedata.com), used by
 * {@link com.pixel.portfolio.loader.HistoricalDataLoader} to back-fill demo symbols that have no
 * CSV seed and no rows in price_history yet. Free tier: 800 requests/day, 8 requests/minute.
 */
@Service
public class TwelveDataHistoricalService {

    private static final Logger log = LoggerFactory.getLogger(TwelveDataHistoricalService.class);

    private final RestClient restClient;
    private final String apiKey;

    public TwelveDataHistoricalService(RestClient twelveDataRestClient,
                                        @Value("${twelvedata.api-key:}") String apiKey) {
        this.restClient = twelveDataRestClient;
        this.apiKey = apiKey;
        if (!hasApiKey()) {
            log.warn("TWELVEDATA_API_KEY is not set — historical price backfill will use synthetic data only");
        }
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Fetches full daily OHLCV history for a symbol. Returns an empty list on any failure
     * (missing key, unknown symbol, rate limit, network error) so callers can fall back to
     * synthetic data gracefully instead of failing startup.
     */
    public List<PriceHistory> fetchDailyHistory(String symbol) {
        if (!hasApiKey()) return List.of();
        String sym = symbol.toUpperCase(Locale.ROOT);
        try {
            TwelveDataTimeSeriesResponse response = restClient.get()
                    .uri(uri -> uri.path("/time_series")
                            .queryParam("symbol", sym)
                            .queryParam("interval", "1day")
                            .queryParam("outputsize", "5000")
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(TwelveDataTimeSeriesResponse.class);

            if (response == null || response.values() == null || !"ok".equalsIgnoreCase(response.status())) {
                log.warn("Twelve Data returned no usable history for {}: {}", sym,
                        response != null ? response.message() : "null response");
                return List.of();
            }

            List<PriceHistory> rows = new ArrayList<>();
            for (TwelveDataValue v : response.values()) {
                try {
                    LocalDate date = LocalDate.parse(v.datetime());
                    BigDecimal open = new BigDecimal(v.open());
                    BigDecimal high = new BigDecimal(v.high());
                    BigDecimal low = new BigDecimal(v.low());
                    BigDecimal close = new BigDecimal(v.close());
                    Long volume = v.volume() != null ? Long.parseLong(v.volume()) : null;
                    rows.add(new PriceHistory(sym, date, open, high, low, close, close, volume));
                } catch (Exception rowEx) {
                    log.debug("Skipping malformed Twelve Data row for {}: {}", sym, v);
                }
            }
            return rows;
        } catch (Exception e) {
            log.warn("Twelve Data history fetch failed for {}: {}", sym, e.getMessage());
            return List.of();
        }
    }
}
