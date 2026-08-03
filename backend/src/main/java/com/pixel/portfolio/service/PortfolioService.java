package com.pixel.portfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import com.pixel.portfolio.dto.HoldingResponse;
import com.pixel.portfolio.dto.PortfolioRequest;
import com.pixel.portfolio.dto.PortfolioResponse;
import com.pixel.portfolio.dto.TradeResponse;
import com.pixel.portfolio.entity.Holding;
import com.pixel.portfolio.entity.Portfolio;
import com.pixel.portfolio.entity.Trade;
import com.pixel.portfolio.exception.BusinessException;
import com.pixel.portfolio.exception.ResourceNotFoundException;
import com.pixel.portfolio.repository.HoldingRepository;
import com.pixel.portfolio.repository.PortfolioRepository;
import com.pixel.portfolio.repository.TradeRepository;
import com.pixel.portfolio.util.CsvUtil;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class PortfolioService {

	private final PortfolioRepository portfolioRepository;
	private final HoldingRepository holdingRepository;
	private final TradeRepository tradeRepository;
	private final MarketService marketService;

	public PortfolioService(PortfolioRepository portfolioRepository, HoldingRepository holdingRepository,
			TradeRepository tradeRepository, MarketService marketService) {
		this.portfolioRepository = portfolioRepository;
		this.holdingRepository = holdingRepository;
		this.tradeRepository = tradeRepository;
		this.marketService = marketService;
	}

	public PortfolioResponse createPortfolio(PortfolioRequest request) {
		String name = normalizeName(request.getName());
		if (portfolioRepository.existsByNameIgnoreCase(name)) {
			throw new BusinessException("Portfolio name already exists: " + name);
		}
		Portfolio portfolio = Portfolio.builder()
				.name(name)
				.cashBalance(scaleMoney(request.getAmount()))
				.build();
		Portfolio saved = portfolioRepository.save(portfolio);
		return toResponse(saved);
	}

	public List<PortfolioResponse> getAllPortfolios() {
		return portfolioRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
	}

	public PortfolioResponse getPortfolio(Long id) {
		return toResponse(findPortfolio(id));
	}

	public void deletePortfolio(Long id) {
		Portfolio portfolio = findPortfolio(id);
		if (holdingRepository.existsByPortfolioId(id)) {
			throw new BusinessException("Cannot delete portfolio with active holdings");
		}
		portfolioRepository.delete(portfolio);
	}

	public PortfolioResponse addFunds(Long id, BigDecimal amount) {
		Portfolio portfolio = findPortfolio(id);
		BigDecimal normalized = positiveMoney(amount, "amount");
		portfolio.setCashBalance(scaleMoney(portfolio.getCashBalance().add(normalized)));
		return toResponse(portfolioRepository.save(portfolio));
	}

	public PortfolioResponse withdrawFunds(Long id, BigDecimal amount) {
		Portfolio portfolio = findPortfolio(id);
		BigDecimal normalized = positiveMoney(amount, "amount");
		if (portfolio.getCashBalance().compareTo(normalized) < 0) {
			throw new BusinessException("Insufficient cash balance");
		}
		portfolio.setCashBalance(scaleMoney(portfolio.getCashBalance().subtract(normalized)));
		return toResponse(portfolioRepository.save(portfolio));
	}

	@Transactional
	public List<HoldingResponse> getHoldings(Long portfolioId) {
		Portfolio portfolio = findPortfolio(portfolioId);
		return holdingRepository.findByPortfolioIdOrderBySymbolAsc(portfolio.getId()).stream()
				.map(this::toHoldingResponse)
				.collect(Collectors.toList());
	}

	@Transactional
	public List<TradeResponse> getTrades(Long portfolioId) {
		findPortfolio(portfolioId);
		return tradeRepository.findByPortfolioIdOrderByCreatedAtDesc(portfolioId).stream().map(this::toTradeResponse)
				.collect(Collectors.toList());
	}

	@Transactional
	public String exportStatementCsv(Long portfolioId) {
		PortfolioResponse portfolio = getPortfolio(portfolioId);
		List<HoldingResponse> holdings = getHoldings(portfolioId);
		var pnl = new com.pixel.portfolio.dto.PortfolioPnLResponse();
		pnl.setPortfolioId(portfolio.getId());
		pnl.setPortfolioName(portfolio.getName());
		pnl.setCashBalance(portfolio.getCashBalance());
		pnl.setHoldings(holdings);
		return CsvUtil.portfolioStatementCsv(enrichStatement(portfolio, holdings));
	}

	public String buildPortfolioContext(Long portfolioId) {
		PortfolioResponse portfolio = getPortfolio(portfolioId);
		List<HoldingResponse> holdings = getHoldings(portfolioId);
		List<TradeResponse> trades = getTrades(portfolioId);
		return "Portfolio " + portfolio.getName() + " has cash balance " + portfolio.getCashBalance()
				+ ", holdings count " + holdings.size() + ", recent trades " + trades.size() + ".";
	}

	private com.pixel.portfolio.dto.PortfolioPnLResponse enrichStatement(PortfolioResponse portfolio,
			List<HoldingResponse> holdings) {
		BigDecimal costBasis = BigDecimal.ZERO;
		BigDecimal marketValue = BigDecimal.ZERO;
		for (HoldingResponse holding : holdings) {
			costBasis = costBasis.add(holding.getAveragePrice().multiply(holding.getQuantity()));
			marketValue = marketValue.add(holding.getMarketValue());
		}
		BigDecimal totalValue = scaleMoney(portfolio.getCashBalance().add(marketValue));
		BigDecimal totalPnl = scaleMoney(marketValue.subtract(costBasis));
		BigDecimal pnlPercentage = costBasis.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: totalPnl.divide(costBasis, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
		return com.pixel.portfolio.dto.PortfolioPnLResponse.builder()
				.portfolioId(portfolio.getId())
				.portfolioName(portfolio.getName())
				.cashBalance(portfolio.getCashBalance())
				.costBasis(scaleMoney(costBasis))
				.currentMarketValue(scaleMoney(marketValue))
				.totalPortfolioValue(totalValue)
				.totalPnl(totalPnl)
				.pnlPercentage(scaleMoney(pnlPercentage))
				.holdings(holdings)
				.build();
	}

	private Portfolio findPortfolio(Long id) {
		return portfolioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + id));
	}

	private PortfolioResponse toResponse(Portfolio portfolio) {
		return PortfolioResponse.builder()
				.id(portfolio.getId())
				.name(portfolio.getName())
				.cashBalance(scaleMoney(portfolio.getCashBalance()))
				.holdingsCount(portfolio.getHoldings() == null ? 0 : portfolio.getHoldings().size())
				.tradesCount(portfolio.getTrades() == null ? 0 : portfolio.getTrades().size())
				.build();
	}

	private HoldingResponse toHoldingResponse(Holding holding) {
		BigDecimal marketPrice = marketService.getLivePrice(holding.getSymbol()).getPrice();
		BigDecimal marketValue = scaleMoney(marketPrice.multiply(holding.getQuantity()));
		BigDecimal pnl = scaleMoney(marketValue.subtract(holding.getAveragePrice().multiply(holding.getQuantity())));
		return HoldingResponse.builder()
				.symbol(holding.getSymbol())
				.quantity(holding.getQuantity())
				.averagePrice(scaleMoney(holding.getAveragePrice()))
				.marketPrice(scaleMoney(marketPrice))
				.marketValue(marketValue)
				.pnl(pnl)
				.build();
	}

	private TradeResponse toTradeResponse(Trade trade) {
		return TradeResponse.builder()
				.id(trade.getId())
				.portfolioId(trade.getPortfolio() == null ? null : trade.getPortfolio().getId())
				.symbol(trade.getSymbol())
				.quantity(trade.getQuantity())
				.price(scaleMoney(trade.getPrice()))
				.type(trade.getType())
				.createdAt(trade.getCreatedAt())
				.tradeValue(scaleMoney(trade.getPrice().multiply(trade.getQuantity())))
				.build();
	}

	private String normalizeName(String name) {
		if (!StringUtils.hasText(name)) {
			throw new BusinessException("Portfolio name is required");
		}
		return name.trim();
	}

	private BigDecimal positiveMoney(BigDecimal amount, String fieldName) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new BusinessException(fieldName + " must be greater than zero");
		}
		return scaleMoney(amount);
	}

	private BigDecimal scaleMoney(BigDecimal value) {
		if (value == null) {
			return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
		}
		return value.setScale(4, RoundingMode.HALF_UP);
	}
}

