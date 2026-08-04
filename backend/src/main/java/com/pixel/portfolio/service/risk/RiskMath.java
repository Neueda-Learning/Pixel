package com.pixel.portfolio.service.risk;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure risk-metric formulas over daily close prices. No Spring, no I/O —
 * kept side-effect-free so the math can be unit-tested directly.
 */
public final class RiskMath {

    public static final double TRADING_DAYS_PER_YEAR = 252.0;
    public static final double RISK_FREE_RATE = 0.04;

    private RiskMath() {}

    /** r_t = (close_t - close_{t-1}) / close_{t-1}, for closes in ascending date order. */
    public static List<Double> dailyReturns(List<BigDecimal> closesAsc) {
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < closesAsc.size(); i++) {
            double prev = closesAsc.get(i - 1).doubleValue();
            double curr = closesAsc.get(i).doubleValue();
            if (prev == 0) continue;
            returns.add((curr - prev) / prev);
        }
        return returns;
    }

    public static double mean(List<Double> xs) {
        if (xs.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double x : xs) sum += x;
        return sum / xs.size();
    }

    /** Sample standard deviation (n-1 denominator), 0 if fewer than 2 points. */
    public static double stddev(List<Double> xs) {
        if (xs.size() < 2) return 0.0;
        double m = mean(xs);
        double sumSq = 0.0;
        for (double x : xs) sumSq += (x - m) * (x - m);
        return Math.sqrt(sumSq / (xs.size() - 1));
    }

    public static double annualizedVolatility(List<Double> dailyReturns) {
        return stddev(dailyReturns) * Math.sqrt(TRADING_DAYS_PER_YEAR);
    }

    public static double annualizedReturn(List<Double> dailyReturns) {
        return mean(dailyReturns) * TRADING_DAYS_PER_YEAR;
    }

    public static double sharpeRatio(double annualizedReturn, double annualizedVolatility) {
        if (annualizedVolatility == 0) return 0.0;
        return (annualizedReturn - RISK_FREE_RATE) / annualizedVolatility;
    }

    /** Largest peak-to-trough decline over the window, expressed as a negative fraction (e.g. -0.32). */
    public static double maxDrawdown(List<BigDecimal> closesAsc) {
        if (closesAsc.isEmpty()) return 0.0;
        double peak = closesAsc.get(0).doubleValue();
        double worst = 0.0;
        for (BigDecimal c : closesAsc) {
            double price = c.doubleValue();
            if (price > peak) peak = price;
            if (peak > 0) {
                double drawdown = (price - peak) / peak;
                if (drawdown < worst) worst = drawdown;
            }
        }
        return worst;
    }

    public static double covariance(List<Double> x, List<Double> y) {
        int n = Math.min(x.size(), y.size());
        if (n < 2) return 0.0;
        double meanX = mean(x.subList(0, n));
        double meanY = mean(y.subList(0, n));
        double sum = 0.0;
        for (int i = 0; i < n; i++) {
            sum += (x.get(i) - meanX) * (y.get(i) - meanY);
        }
        return sum / (n - 1);
    }

    public static double variance(List<Double> x) {
        return covariance(x, x);
    }

    /** beta = cov(assetReturns, marketReturns) / var(marketReturns); both lists must already be date-aligned. */
    public static double beta(List<Double> assetReturns, List<Double> marketReturns) {
        double marketVariance = variance(marketReturns);
        if (marketVariance == 0) return 0.0;
        return covariance(assetReturns, marketReturns) / marketVariance;
    }

    /** Simple moving average of the most recent `period` closes, or null if not enough history. */
    public static Double sma(List<BigDecimal> closesAsc, int period) {
        if (closesAsc.size() < period) return null;
        List<BigDecimal> window = closesAsc.subList(closesAsc.size() - period, closesAsc.size());
        double sum = 0.0;
        for (BigDecimal c : window) sum += c.doubleValue();
        return sum / period;
    }

    /** RSI(14) using simple (non-Wilder-smoothed) averaging over the most recent 14 daily changes. */
    public static Double rsi14(List<BigDecimal> closesAsc) {
        int period = 14;
        if (closesAsc.size() < period + 1) return null;
        List<BigDecimal> window = closesAsc.subList(closesAsc.size() - (period + 1), closesAsc.size());
        double gainSum = 0.0;
        double lossSum = 0.0;
        for (int i = 1; i < window.size(); i++) {
            double change = window.get(i).doubleValue() - window.get(i - 1).doubleValue();
            if (change >= 0) gainSum += change;
            else lossSum += -change;
        }
        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;
        if (avgLoss == 0) return 100.0;
        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }
}

