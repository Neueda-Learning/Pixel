package com.pixel.portfolio.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.pixel.portfolio.dto.MarketPriceResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MarketServiceImpl implements MarketService {

	private final List<MarketPriceProvider> providers;
	private final String defaultCurrency;

	public MarketServiceImpl(List<MarketPriceProvider> providers,
			@Value("${app.market.default-currency:USD}") String defaultCurrency) {
		this.providers = providers == null ? List.of() : providers.stream()
				.sorted(Comparator.comparingInt(provider -> 1000))
				.toList();
		this.defaultCurrency = defaultCurrency;
	}

	@Override
	public MarketPriceResponse getLivePrice(String symbol) {
		String normalized = normalize(symbol);
		MarketPriceProvider provider = providers.stream().filter(item -> item.supports(normalized)).findFirst().orElse(null);
		if (provider == null) {
			provider = new MockMarketPriceProvider();
		}
		BigDecimal price = provider.getPrice(normalized);
		return MarketPriceResponse.builder()
				.symbol(normalized)
				.price(price)
				.provider(provider.getName() + "-" + defaultCurrency)
				.timestamp(Instant.now())
				.build();
	}

	private String normalize(String symbol) {
		if (!StringUtils.hasText(symbol)) {
			throw new IllegalArgumentException("Symbol is required");
		}
		return symbol.trim().toUpperCase();
	}
}

