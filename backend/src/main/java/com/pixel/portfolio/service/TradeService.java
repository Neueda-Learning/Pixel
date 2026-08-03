package com.pixel.portfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import com.pixel.portfolio.dto.TradeRequest;
import com.pixel.portfolio.dto.TradeResponse;
import com.pixel.portfolio.entity.Holding;
import com.pixel.portfolio.entity.Portfolio;
import com.pixel.portfolio.entity.Trade;
import com.pixel.portfolio.entity.TradeType;
import com.pixel.portfolio.exception.BusinessException;
import com.pixel.portfolio.exception.ResourceNotFoundException;
import com.pixel.portfolio.repository.HoldingRepository;
import com.pixel.portfolio.repository.PortfolioRepository;
import com.pixel.portfolio.repository.TradeRepository;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TradeService {

	private final PortfolioRepository portfolioRepository;
	private final HoldingRepository holdingRepository;
	private final TradeRepository tradeRepository;
	private final MarketService marketService;

	public TradeService(PortfolioRepository portfolioRepository, HoldingRepository holdingRepository,
			TradeRepository tradeRepository, MarketService marketService) {
		this.portfolioRepository = portfolioRepository;
		this.holdingRepository = holdingRepository;
		this.tradeRepository = tradeRepository;
		this.marketService = marketService;
	}

	public TradeResponse buy(TradeRequest request) {
		return execute(request, TradeType.BUY);
	}

	public TradeResponse sell(TradeRequest request) {
		return execute(request, TradeType.SELL);
	}

	public List<TradeResponse> getTradesByPortfolio(Long portfolioId) {
		findPortfolio(portfolioId);
		return tradeRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId).stream().map(this::toResponse)
				.collect(Collectors.toList());
	}

	private TradeResponse execute(TradeRequest request, TradeType type) {
		Portfolio portfolio = findPortfolio(request.getPortfolioId());
		String symbol = normalizeSymbol(request.getSymbol());
		BigDecimal quantity = scaleQuantity(request.getQuantity());
		BigDecimal price = request.getPrice() == null ? marketService.getLivePrice(symbol).getPrice() : scaleMoney(request.getPrice());
		if (price.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException("Price must be greater than zero");
		}

		BigDecimal tradeValue = scaleMoney(price.multiply(quantity));
		Holding holding = holdingRepository.findByPortfolioIdAndSymbolIgnoreCase(portfolio.getId(), symbol).orElse(null);

		if (type == TradeType.BUY) {
			if (portfolio.getCashBalance().compareTo(tradeValue) < 0) {
				throw new BusinessException("Insufficient cash balance for buy order");
			}
			portfolio.setCashBalance(scaleMoney(portfolio.getCashBalance().subtract(tradeValue)));
			if (holding == null) {
				holding = Holding.builder()
						.portfolio(portfolio)
						.symbol(symbol)
						.quantity(quantity)
						.averagePrice(price)
						.build();
			} else {
				BigDecimal previousQuantity = holding.getQuantity();
				BigDecimal newQuantity = scaleQuantity(previousQuantity.add(quantity));
				BigDecimal weightedCost = previousQuantity.multiply(holding.getAveragePrice()).add(quantity.multiply(price));
				holding.setQuantity(newQuantity);
				holding.setAveragePrice(scaleMoney(weightedCost.divide(newQuantity, 4, RoundingMode.HALF_UP)));
			}
			holdingRepository.save(holding);
		} else {
			if (holding == null) {
				throw new BusinessException("Cannot sell a symbol that is not in the portfolio: " + symbol);
			}
			if (holding.getQuantity().compareTo(quantity) < 0) {
				throw new BusinessException("Cannot sell more quantity than available");
			}
			portfolio.setCashBalance(scaleMoney(portfolio.getCashBalance().add(tradeValue)));
			BigDecimal remainingQuantity = scaleQuantity(holding.getQuantity().subtract(quantity));
			if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
				holdingRepository.delete(holding);
			} else {
				holding.setQuantity(remainingQuantity);
				holdingRepository.save(holding);
			}
		}

		portfolioRepository.save(portfolio);
		Trade trade = Trade.builder()
				.portfolio(portfolio)
				.symbol(symbol)
				.quantity(quantity)
				.price(price)
				.type(type.name())
				.build();
		Trade saved = tradeRepository.save(trade);
		return toResponse(saved);
	}

	private Portfolio findPortfolio(Long id) {
		return portfolioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + id));
	}

	private TradeResponse toResponse(Trade trade) {
		return TradeResponse.builder()
				.id(trade.getId())
				.portfolioId(trade.getPortfolio() == null ? null : trade.getPortfolio().getId())
				.symbol(trade.getSymbol())
				.quantity(scaleQuantity(trade.getQuantity()))
				.price(scaleMoney(trade.getPrice()))
				.type(trade.getType())
				.createdAt(trade.getCreatedAt())
				.tradeValue(scaleMoney(trade.getPrice().multiply(trade.getQuantity())))
				.build();
	}

	private String normalizeSymbol(String symbol) {
		if (!StringUtils.hasText(symbol)) {
			throw new BusinessException("Symbol is required");
		}
		return symbol.trim().toUpperCase();
	}

	private BigDecimal scaleMoney(BigDecimal value) {
		if (value == null) {
			return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
		}
		return value.setScale(4, RoundingMode.HALF_UP);
	}

	private BigDecimal scaleQuantity(BigDecimal value) {
		if (value == null) {
			return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
		}
		return value.setScale(6, RoundingMode.HALF_UP);
	}
}

