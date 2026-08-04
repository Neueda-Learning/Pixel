package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.NewsItemDto;
import com.pixel.portfolio.dto.ProfileDto;
import com.pixel.portfolio.dto.QuoteDto;
import com.pixel.portfolio.dto.SearchResultDto;
import com.pixel.portfolio.exception.ResourceNotFoundException;
import com.pixel.portfolio.integration.finnhub.FinnhubNewsItem;
import com.pixel.portfolio.integration.finnhub.FinnhubProfile;
import com.pixel.portfolio.integration.finnhub.FinnhubQuote;
import com.pixel.portfolio.integration.finnhub.FinnhubSearchResponse;
import com.pixel.portfolio.model.Instrument;
import com.pixel.portfolio.repository.InstrumentRepository;
import com.pixel.portfolio.repository.PriceHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class FinnhubMarketDataService implements MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(FinnhubMarketDataService.class);
    private static final DateTimeFormatter NEWS_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestClient restClient;
    private final String apiKey;
    private final PriceHistoryRepository priceHistoryRepository;
    private final InstrumentRepository instrumentRepository;

    public FinnhubMarketDataService(RestClient finnhubRestClient,
                                     @Value("${finnhub.api-key:}") String apiKey,
                                     PriceHistoryRepository priceHistoryRepository,
                                     InstrumentRepository instrumentRepository) {
        this.restClient = finnhubRestClient;
        this.apiKey = apiKey;
        this.priceHistoryRepository = priceHistoryRepository;
        this.instrumentRepository = instrumentRepository;
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("FINNHUB_API_KEY is not set — market data endpoints will use the price_history DB fallback only");
        }
    }

    private boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    @Cacheable(cacheNames = "quotes", key = "#symbol")
    public QuoteDto getQuote(String symbol) {
        String sym = symbol.toUpperCase(Locale.ROOT);
        if (hasApiKey()) {
            try {
                FinnhubQuote q = restClient.get()
                        .uri(uri -> uri.path("/quote").queryParam("symbol", sym).queryParam("token", apiKey).build())
                        .retrieve()
                        .body(FinnhubQuote.class);
                if (q != null && q.c() != null && q.c().signum() != 0) {
                    return new QuoteDto(sym, q.c(), q.d(), q.dp(), q.h(), q.l(), q.o(), q.pc(), "LIVE", Instant.now());
                }
            } catch (Exception e) {
                log.warn("Finnhub quote lookup failed for {}: {}", sym, e.getMessage());
            }
        }
        return dbFallbackQuote(sym);
    }

    private QuoteDto dbFallbackQuote(String symbol) {
        return priceHistoryRepository.findTopBySymbolOrderByTradeDateDesc(symbol)
                .map(p -> new QuoteDto(symbol, p.getClose(), null, null, p.getHigh(), p.getLow(), p.getOpen(), null,
                        "DB_FALLBACK", p.getTradeDate().atStartOfDay(ZoneOffset.UTC).toInstant()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No live quote or historical price available for " + symbol));
    }

    @Override
    @Cacheable(cacheNames = "profiles", key = "#symbol")
    public ProfileDto getProfile(String symbol) {
        String sym = symbol.toUpperCase(Locale.ROOT);
        if (hasApiKey()) {
            try {
                FinnhubProfile p = restClient.get()
                        .uri(uri -> uri.path("/stock/profile2").queryParam("symbol", sym).queryParam("token", apiKey).build())
                        .retrieve()
                        .body(FinnhubProfile.class);
                if (p != null && p.name() != null && !p.name().isBlank()) {
                    return new ProfileDto(sym, p.name(), p.logo(), p.exchange(), p.currency(), "LIVE");
                }
            } catch (Exception e) {
                log.warn("Finnhub profile lookup failed for {}: {}", sym, e.getMessage());
            }
        }
        return dbFallbackProfile(sym);
    }

    private ProfileDto dbFallbackProfile(String symbol) {
        Instrument instrument = instrumentRepository.findById(symbol).orElse(null);
        String name = instrument != null ? instrument.getName() : symbol;
        String currency = instrument != null ? instrument.getCurrency() : "USD";
        return new ProfileDto(symbol, name, null, null, currency, "DB_FALLBACK");
    }

    @Override
    @Cacheable(cacheNames = "news", key = "#symbol")
    public List<NewsItemDto> getNews(String symbol) {
        String sym = symbol.toUpperCase(Locale.ROOT);
        if (!hasApiKey()) return List.of();
        try {
            LocalDate to = LocalDate.now();
            LocalDate from = to.minusDays(30);
            List<FinnhubNewsItem> items = restClient.get()
                    .uri(uri -> uri.path("/company-news")
                            .queryParam("symbol", sym)
                            .queryParam("from", NEWS_DATE_FORMAT.format(from))
                            .queryParam("to", NEWS_DATE_FORMAT.format(to))
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<FinnhubNewsItem>>() {});
            if (items == null) return List.of();
            return items.stream()
                    .limit(20)
                    .map(i -> new NewsItemDto(i.id(), i.headline(), i.summary(), i.url(), i.source(), i.image(),
                            Instant.ofEpochSecond(i.datetime())))
                    .toList();
        } catch (Exception e) {
            log.warn("Finnhub news lookup failed for {}: {}", sym, e.getMessage());
            return List.of();
        }
    }

    @Override
    @Cacheable(cacheNames = "search", key = "#query")
    public List<SearchResultDto> search(String query) {
        if (hasApiKey()) {
            try {
                FinnhubSearchResponse resp = restClient.get()
                        .uri(uri -> uri.path("/search").queryParam("q", query).queryParam("token", apiKey).build())
                        .retrieve()
                        .body(FinnhubSearchResponse.class);
                if (resp != null && resp.result() != null && !resp.result().isEmpty()) {
                    return resp.result().stream()
                            .limit(15)
                            .map(r -> new SearchResultDto(r.symbol(), r.description(), r.type()))
                            .toList();
                }
            } catch (Exception e) {
                log.warn("Finnhub search failed for '{}': {}", query, e.getMessage());
            }
        }
        return searchLocalInstruments(query);
    }

    private List<SearchResultDto> searchLocalInstruments(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        return instrumentRepository.findAll().stream()
                .filter(i -> i.getSymbol().toLowerCase(Locale.ROOT).contains(q)
                        || (i.getName() != null && i.getName().toLowerCase(Locale.ROOT).contains(q)))
                .map(i -> new SearchResultDto(i.getSymbol(), i.getName(), i.getAssetType()))
                .toList();
    }
}

