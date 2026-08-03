package com.pixel.portfolio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.pixel.portfolio.dto.HoldingResponse;
import com.pixel.portfolio.dto.HoldingWeightResponse;
import com.pixel.portfolio.dto.PortfolioDiversityResponse;
import com.pixel.portfolio.dto.PortfolioHealthRequest;
import com.pixel.portfolio.dto.PortfolioHealthResponse;
import com.pixel.portfolio.dto.PortfolioPnLResponse;
import com.pixel.portfolio.dto.PortfolioRiskRequest;
import com.pixel.portfolio.dto.PortfolioRiskResponse;
import com.pixel.portfolio.dto.WhatIfRequest;
import com.pixel.portfolio.dto.WhatIfResponse;
import com.pixel.portfolio.entity.Holding;
import com.pixel.portfolio.entity.Portfolio;
import com.pixel.portfolio.entity.TradeType;
import com.pixel.portfolio.exception.BusinessException;
import com.pixel.portfolio.exception.ResourceNotFoundException;
import com.pixel.portfolio.repository.HoldingRepository;
import com.pixel.portfolio.repository.PortfolioRepository;
import com.pixel.portfolio.util.CsvUtil;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AnalyticsService {

	private final PortfolioRepository portfolioRepository;
	private final HoldingRepository holdingRepository;
	private final MarketService marketService;

	public AnalyticsService(PortfolioRepository portfolioRepository, HoldingRepository holdingRepository,
			MarketService marketService) {
		this.portfolioRepository = portfolioRepository;
		this.holdingRepository = holdingRepository;
		this.marketService = marketService;
	}

	public PortfolioPnLResponse getPnL(Long portfolioId) {
		Portfolio portfolio = findPortfolio(requiredPortfolioId(portfolioId));
		List<HoldingResponse> holdings = getHoldingResponses(portfolioId);
		BigDecimal costBasis = BigDecimal.ZERO;
		BigDecimal marketValue = BigDecimal.ZERO;
		for (HoldingResponse holding : holdings) {
			costBasis = costBasis.add(holding.getAveragePrice().multiply(holding.getQuantity()));
			marketValue = marketValue.add(holding.getMarketValue());
		}
		BigDecimal totalValue = scaleMoney(portfolio.getCashBalance().add(marketValue));
		BigDecimal totalPnl = scaleMoney(marketValue.subtract(costBasis));
		BigDecimal pnlPct = costBasis.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
				: totalPnl.divide(costBasis, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
		return PortfolioPnLResponse.builder()
				.portfolioId(portfolio.getId())
				.portfolioName(portfolio.getName())
				.cashBalance(scaleMoney(portfolio.getCashBalance()))
				.costBasis(scaleMoney(costBasis))
				.currentMarketValue(scaleMoney(marketValue))
				.totalPortfolioValue(totalValue)
				.totalPnl(totalPnl)
				.pnlPercentage(scaleMoney(pnlPct))
				.holdings(holdings)
				.build();
	}

	public PortfolioHealthResponse getHealth(PortfolioHealthRequest request) {
		Long portfolioId = request == null ? null : request.getPortfolioId();
		PortfolioPnLResponse pnl = getPnL(requiredPortfolioId(portfolioId));
		BigDecimal minimumCashRatio = request == null || request.getMinimumCashRatio() == null ? new BigDecimal("0.10")
				: request.getMinimumCashRatio();
		return buildHealth(pnl, minimumCashRatio);
	}

	public PortfolioHealthResponse getHealth(Long portfolioId) {
		return buildHealth(getPnL(requiredPortfolioId(portfolioId)), new BigDecimal("0.10"));
	}

	public PortfolioRiskResponse getRisk(PortfolioRiskRequest request) {
		Long portfolioId = request == null ? null : request.getPortfolioId();
		PortfolioPnLResponse pnl = getPnL(requiredPortfolioId(portfolioId));
		List<HoldingWeightResponse> weights = calculateWeights(pnl.getHoldings());
		BigDecimal maxWeight = weights.stream().map(HoldingWeightResponse::getWeight).max(Comparator.naturalOrder())
				.orElse(BigDecimal.ZERO);
		BigDecimal stress = request == null || request.getStressPercent() == null ? new BigDecimal("0.15")
				: request.getStressPercent();
		BigDecimal maxAllowed = request == null || request.getMaxSingleHoldingWeight() == null ? new BigDecimal("0.35")
				: request.getMaxSingleHoldingWeight();
		int riskScore = clampToInt(maxWeight.multiply(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(1.1))
				.add(stress.multiply(BigDecimal.valueOf(100))).setScale(0, RoundingMode.HALF_UP).intValue());
		String level = riskScore < 35 ? "LOW" : riskScore < 70 ? "MODERATE" : "HIGH";
		String message = maxWeight.compareTo(maxAllowed) > 0
				? "Largest holding exceeds the recommended concentration threshold."
				: "Risk is within the configured threshold.";
		return PortfolioRiskResponse.builder()
				.portfolioId(requiredPortfolioId(portfolioId))
				.riskScore(riskScore)
				.riskLevel(level)
				.maxSingleHoldingWeight(scaleRatio(maxWeight))
				.stressPercent(scaleRatio(stress))
				.message(message)
				.build();
	}

	public PortfolioDiversityResponse getDiversity(Long portfolioId) {
		portfolioId = requiredPortfolioId(portfolioId);
		Portfolio portfolio = findPortfolio(portfolioId);
		PortfolioPnLResponse pnl = getPnL(portfolioId);
		List<HoldingWeightResponse> weights = calculateWeights(pnl.getHoldings());
		BigDecimal hhi = BigDecimal.ZERO;
		BigDecimal topHoldingWeight = BigDecimal.ZERO;
		for (HoldingWeightResponse weight : weights) {
			hhi = hhi.add(weight.getWeight().multiply(weight.getWeight()));
			if (weight.getWeight().compareTo(topHoldingWeight) > 0) {
				topHoldingWeight = weight.getWeight();
			}
		}
		BigDecimal diversityScore = BigDecimal.ONE.subtract(hhi).multiply(BigDecimal.valueOf(100));
		String concentrationLevel = topHoldingWeight.compareTo(new BigDecimal("0.50")) > 0 ? "HIGH"
				: topHoldingWeight.compareTo(new BigDecimal("0.25")) > 0 ? "MODERATE" : "LOW";
		return PortfolioDiversityResponse.builder()
				.portfolioId(portfolio.getId())
				.portfolioName(portfolio.getName())
				.herfindahlIndex(scaleRatio(hhi))
				.diversityScore(scaleMoney(diversityScore))
				.topHoldingWeight(scaleRatio(topHoldingWeight))
				.concentrationLevel(concentrationLevel)
				.holdings(weights)
				.build();
	}

	public WhatIfResponse whatIf(WhatIfRequest request) {
		if (request == null) {
			throw new BusinessException("What-if request is required");
		}
		PortfolioPnLResponse pnl = getPnL(requiredPortfolioId(request.getPortfolioId()));
		BigDecimal tradeValue = scaleMoney(request.getPrice().multiply(request.getQuantity()));
		BigDecimal beforeCash = pnl.getCashBalance();
		BigDecimal beforeMarket = pnl.getCurrentMarketValue();
		BigDecimal afterCash;
		BigDecimal afterMarket;
		if (request.getType() == TradeType.BUY) {
			if (beforeCash.compareTo(tradeValue) < 0) {
				throw new BusinessException("What-if buy exceeds available cash");
			}
			afterCash = scaleMoney(beforeCash.subtract(tradeValue));
			afterMarket = scaleMoney(beforeMarket.add(tradeValue));
		} else {
			if (beforeMarket.compareTo(tradeValue) < 0) {
				throw new BusinessException("What-if sell exceeds total holdings value");
			}
			afterCash = scaleMoney(beforeCash.add(tradeValue));
			afterMarket = scaleMoney(beforeMarket.subtract(tradeValue));
		}
		BigDecimal projected = scaleMoney(afterCash.add(afterMarket));
		BigDecimal delta = scaleMoney(projected.subtract(pnl.getTotalPortfolioValue()));
		String message = request.getType() == TradeType.BUY
				? "Projected buy executed at the supplied price."
				: "Projected sell executed at the supplied price.";
		return WhatIfResponse.builder()
				.portfolioId(request.getPortfolioId())
				.scenario(request.getType() + " " + request.getSymbol().toUpperCase() + " " + request.getQuantity() + " @ " + scaleMoney(request.getPrice()))
				.beforeCashBalance(beforeCash)
				.afterCashBalance(afterCash)
				.beforeMarketValue(beforeMarket)
				.afterMarketValue(afterMarket)
				.projectedTotalValue(projected)
				.delta(delta)
				.message(message)
				.build();
	}

	public String diversityCsv(Long portfolioId) {
		return CsvUtil.diversityCsv(getDiversity(requiredPortfolioId(portfolioId)));
	}

	private PortfolioHealthResponse buildHealth(PortfolioPnLResponse pnl, BigDecimal minimumCashRatio) {
		BigDecimal totalValue = pnl.getTotalPortfolioValue();
		BigDecimal cashRatio = totalValue.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
				: pnl.getCashBalance().divide(totalValue, 4, RoundingMode.HALF_UP);
		BigDecimal concentration = pnl.getHoldings().stream()
				.map(holding -> holding.getMarketValue().divide(pnl.getCurrentMarketValue().compareTo(BigDecimal.ZERO) == 0
						? BigDecimal.ONE
						: pnl.getCurrentMarketValue(), 4, RoundingMode.HALF_UP))
				.max(Comparator.naturalOrder())
				.orElse(BigDecimal.ZERO);
		int score = 100;
		if (cashRatio.compareTo(minimumCashRatio) < 0) {
			score -= 20;
		}
		if (concentration.compareTo(new BigDecimal("0.35")) > 0) {
			score -= 25;
		}
		if (pnl.getTotalPnl().compareTo(BigDecimal.ZERO) < 0) {
			score -= 10;
		}
		score = Math.max(0, Math.min(100, score));
		String status = score >= 75 ? "HEALTHY" : score >= 50 ? "WATCH" : "CRITICAL";
		String message = cashRatio.compareTo(minimumCashRatio) < 0
				? "Cash ratio is below the recommended threshold."
				: "Cash ratio and concentration are within acceptable bounds.";
		return PortfolioHealthResponse.builder()
				.portfolioId(pnl.getPortfolioId())
				.score(score)
				.status(status)
				.cashRatio(scaleRatio(cashRatio))
				.concentrationRisk(scaleRatio(concentration))
				.message(message)
				.build();
	}

	private List<HoldingResponse> getHoldingResponses(Long portfolioId) {
		return holdingRepository.findByPortfolioIdOrderBySymbolAsc(portfolioId).stream().map(this::toHoldingResponse)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	private HoldingResponse toHoldingResponse(Holding holding) {
		BigDecimal marketPrice = marketService.getLivePrice(holding.getSymbol()).getPrice();
		BigDecimal marketValue = scaleMoney(marketPrice.multiply(holding.getQuantity()));
		BigDecimal costBasis = scaleMoney(holding.getAveragePrice().multiply(holding.getQuantity()));
		BigDecimal pnl = scaleMoney(marketValue.subtract(costBasis));
		return HoldingResponse.builder()
				.symbol(holding.getSymbol())
				.quantity(holding.getQuantity())
				.averagePrice(scaleMoney(holding.getAveragePrice()))
				.marketPrice(scaleMoney(marketPrice))
				.marketValue(marketValue)
				.pnl(pnl)
				.build();
	}

	private List<HoldingWeightResponse> calculateWeights(List<HoldingResponse> holdings) {
		BigDecimal totalMarketValue = holdings.stream().map(HoldingResponse::getMarketValue).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		if (totalMarketValue.compareTo(BigDecimal.ZERO) == 0) {
			return holdings.stream()
					.map(holding -> HoldingWeightResponse.builder().symbol(holding.getSymbol()).weight(BigDecimal.ZERO).marketValue(BigDecimal.ZERO).build())
					.collect(Collectors.toList());
		}
		return holdings.stream()
				.map(holding -> HoldingWeightResponse.builder()
						.symbol(holding.getSymbol())
						.marketValue(scaleMoney(holding.getMarketValue()))
						.weight(scaleRatio(holding.getMarketValue().divide(totalMarketValue, 4, RoundingMode.HALF_UP)))
						.build())
				.collect(Collectors.toList());
	}

	private Portfolio findPortfolio(Long id) {
		return portfolioRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Portfolio not found with id: " + id));
	}

	private Long requiredPortfolioId(Long portfolioId) {
		if (portfolioId == null || portfolioId <= 0) {
			throw new BusinessException("portfolioId is required and must be greater than zero");
		}
		return portfolioId;
	}

	private BigDecimal scaleMoney(BigDecimal value) {
		if (value == null) {
			return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
		}
		return value.setScale(4, RoundingMode.HALF_UP);
	}

	private BigDecimal scaleRatio(BigDecimal value) {
		if (value == null) {
			return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
		}
		return value.setScale(4, RoundingMode.HALF_UP);
	}

	private int clampToInt(int value) {
		return Math.max(0, Math.min(100, value));
	}
}

