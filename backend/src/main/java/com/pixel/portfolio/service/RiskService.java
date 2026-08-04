package com.pixel.portfolio.service;

import com.pixel.portfolio.dto.RiskDto;
import com.pixel.portfolio.exception.BadRequestException;
import com.pixel.portfolio.exception.ResourceNotFoundException;
import com.pixel.portfolio.model.PriceHistory;
import com.pixel.portfolio.repository.PriceHistoryRepository;
import com.pixel.portfolio.service.risk.RiskMath;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Computes transparent, rule-based risk metrics and a BUY/HOLD/AVOID
 * recommendation from price_history close prices. See RiskMath for the
 * underlying formulas.
 */
@Service
public class RiskService {

    private static final String BENCHMARK_SYMBOL = "SPY";

    private final PriceHistoryRepository priceHistoryRepository;

    public RiskService(PriceHistoryRepository priceHistoryRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
    }

    public RiskDto getRisk(String symbol) {
        String sym = symbol.toUpperCase(Locale.ROOT);
        List<PriceHistory> rows = priceHistoryRepository.findBySymbolOrderByTradeDateAsc(sym);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("No price history available for " + sym);
        }

        List<BigDecimal> closes = rows.stream().map(PriceHistory::getClose).toList();
        if (closes.size() < 2) {
            throw new BadRequestException(
                    "Insufficient price history to compute risk metrics for " + sym
                            + " (need at least 2 data points, found " + closes.size() + ")");
        }

        List<Double> returns = RiskMath.dailyReturns(closes);
        double volatility = RiskMath.annualizedVolatility(returns);
        double annualizedReturn = RiskMath.annualizedReturn(returns);
        double sharpe = RiskMath.sharpeRatio(annualizedReturn, volatility);
        double maxDrawdown = RiskMath.maxDrawdown(closes);
        Double sma50 = RiskMath.sma(closes, 50);
        Double sma200 = RiskMath.sma(closes, 200);
        Double rsi14 = RiskMath.rsi14(closes);
        Double beta = computeBeta(sym, rows);

        String trend = trendFor(sma50, sma200);
        String recommendation = recommend(sharpe, volatility, maxDrawdown);
        String rationale = buildRationale(recommendation, sharpe, volatility, maxDrawdown);

        return new RiskDto(
                sym,
                rows.get(rows.size() - 1).getTradeDate(),
                closes.size(),
                round(volatility), round(annualizedReturn), round(sharpe), round(maxDrawdown), beta == null ? null : round(beta),
                sma50 == null ? null : round(sma50), sma200 == null ? null : round(sma200),
                trend,
                rsi14 == null ? null : round(rsi14),
                recommendation, rationale);
    }

    private Double computeBeta(String symbol, List<PriceHistory> assetRows) {
        if (BENCHMARK_SYMBOL.equals(symbol)) return 1.0;

        List<PriceHistory> benchmarkRows = priceHistoryRepository.findBySymbolOrderByTradeDateAsc(BENCHMARK_SYMBOL);
        if (benchmarkRows.size() < 2) return null;

        Map<LocalDate, Double> assetReturns = returnsByDate(assetRows);
        Map<LocalDate, Double> benchmarkReturns = returnsByDate(benchmarkRows);

        List<Double> alignedAsset = new ArrayList<>();
        List<Double> alignedBenchmark = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> entry : assetReturns.entrySet()) {
            Double benchmarkReturn = benchmarkReturns.get(entry.getKey());
            if (benchmarkReturn != null) {
                alignedAsset.add(entry.getValue());
                alignedBenchmark.add(benchmarkReturn);
            }
        }
        if (alignedAsset.size() < 2) return null;
        return RiskMath.beta(alignedAsset, alignedBenchmark);
    }

    private Map<LocalDate, Double> returnsByDate(List<PriceHistory> rowsAsc) {
        Map<LocalDate, Double> byDate = new LinkedHashMap<>();
        for (int i = 1; i < rowsAsc.size(); i++) {
            double prev = rowsAsc.get(i - 1).getClose().doubleValue();
            double curr = rowsAsc.get(i).getClose().doubleValue();
            if (prev == 0) continue;
            byDate.put(rowsAsc.get(i).getTradeDate(), (curr - prev) / prev);
        }
        return byDate;
    }

    private String trendFor(Double sma50, Double sma200) {
        if (sma50 == null || sma200 == null) return "UNKNOWN";
        if (sma50 > sma200) return "BULLISH";
        if (sma50 < sma200) return "BEARISH";
        return "NEUTRAL";
    }

    private String recommend(double sharpe, double volatility, double maxDrawdown) {
        if (sharpe > 1.0 && volatility < 0.30) return "BUY";
        if (volatility > 0.45 || maxDrawdown < -0.40) return "AVOID";
        return "HOLD";
    }

    private String buildRationale(String recommendation, double sharpe, double volatility, double maxDrawdown) {
        String volPct = pct(volatility);
        String ddPct = pct(maxDrawdown);
        return switch (recommendation) {
            case "BUY" -> String.format(Locale.ROOT,
                    "Sharpe ratio of %.2f exceeds 1.0 and annualized volatility of %s is under the 30%% threshold — "
                            + "risk-adjusted returns look attractive.", sharpe, volPct);
            case "AVOID" -> volatility > 0.45
                    ? String.format(Locale.ROOT,
                    "Annualized volatility of %s exceeds the 45%% threshold, indicating high risk relative to typical holdings.", volPct)
                    : String.format(Locale.ROOT,
                    "Maximum drawdown of %s exceeds the -40%% threshold, indicating a severe historical peak-to-trough decline.", ddPct);
            default -> String.format(Locale.ROOT,
                    "Sharpe ratio of %.2f, annualized volatility of %s, and max drawdown of %s fall between the BUY and AVOID "
                            + "thresholds — no strong signal either way.", sharpe, volPct, ddPct);
        };
    }

    private String pct(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value * 100);
    }

    private double round(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}

