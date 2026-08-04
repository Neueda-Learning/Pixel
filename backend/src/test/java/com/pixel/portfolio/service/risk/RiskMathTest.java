package com.pixel.portfolio.service.risk;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskMathTest {

    private static List<BigDecimal> closes(double... values) {
        return java.util.Arrays.stream(values).mapToObj(BigDecimal::valueOf).toList();
    }

    @Test
    void dailyReturns_computesPercentChangeBetweenConsecutiveCloses() {
        List<Double> returns = RiskMath.dailyReturns(closes(100, 110, 99));
        assertEquals(2, returns.size());
        assertEquals(0.10, returns.get(0), 1e-9);
        assertEquals(-0.10, returns.get(1), 1e-9);
    }

    @Test
    void mean_averagesValues() {
        assertEquals(2.0, RiskMath.mean(List.of(1.0, 2.0, 3.0)), 1e-9);
    }

    @Test
    void stddev_usesSampleDenominator() {
        // values 2,4,4,4,5,5,7,9 -> known sample stddev = 2.13809...
        List<Double> xs = List.of(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0);
        assertEquals(2.13809, RiskMath.stddev(xs), 1e-4);
    }

    @Test
    void stddev_singleValue_isZero() {
        assertEquals(0.0, RiskMath.stddev(List.of(1.0)), 1e-9);
    }

    @Test
    void annualizedVolatility_scalesStddevBySqrt252() {
        List<Double> returns = List.of(0.01, -0.01, 0.02, -0.02);
        double expected = RiskMath.stddev(returns) * Math.sqrt(252);
        assertEquals(expected, RiskMath.annualizedVolatility(returns), 1e-9);
    }

    @Test
    void annualizedReturn_scalesMeanBy252() {
        List<Double> returns = List.of(0.001, 0.002, -0.001);
        double expected = RiskMath.mean(returns) * 252;
        assertEquals(expected, RiskMath.annualizedReturn(returns), 1e-9);
    }

    @Test
    void sharpeRatio_subtractsRiskFreeRateAndDividesByVolatility() {
        double sharpe = RiskMath.sharpeRatio(0.20, 0.10);
        assertEquals((0.20 - 0.04) / 0.10, sharpe, 1e-9);
    }

    @Test
    void sharpeRatio_zeroVolatility_returnsZero() {
        assertEquals(0.0, RiskMath.sharpeRatio(0.20, 0.0), 1e-9);
    }

    @Test
    void maxDrawdown_findsLargestPeakToTroughDecline() {
        // peak 120 -> trough 90 (-25%); peak 130 -> trough 80 (-38.46%, the worst)
        List<BigDecimal> prices = closes(100, 120, 90, 95, 130, 80);
        double dd = RiskMath.maxDrawdown(prices);
        assertEquals((80.0 - 130.0) / 130.0, dd, 1e-9);
    }

    @Test
    void maxDrawdown_monotonicallyRising_isZero() {
        assertEquals(0.0, RiskMath.maxDrawdown(closes(100, 110, 120, 130)), 1e-9);
    }

    @Test
    void beta_ofSeriesTwiceAsVolatileAsMarket_isTwo() {
        List<Double> market = List.of(0.01, 0.02, -0.01, 0.03);
        List<Double> asset = market.stream().map(r -> r * 2).toList();
        assertEquals(2.0, RiskMath.beta(asset, market), 1e-6);
    }

    @Test
    void beta_ofMarketAgainstItself_isOne() {
        List<Double> market = List.of(0.01, 0.02, -0.01, 0.03, -0.005);
        assertEquals(1.0, RiskMath.beta(market, market), 1e-9);
    }

    @Test
    void sma_averagesMostRecentWindow() {
        List<BigDecimal> prices = closes(1, 2, 3, 4, 5);
        assertEquals(4.0, RiskMath.sma(prices, 3)); // (3+4+5)/3
    }

    @Test
    void sma_insufficientHistory_returnsNull() {
        assertNull(RiskMath.sma(closes(1, 2), 3));
    }

    @Test
    void rsi14_allGains_isOneHundred() {
        double[] vals = new double[15];
        for (int i = 0; i < 15; i++) vals[i] = 100 + i; // 14 consecutive +1 days
        assertEquals(100.0, RiskMath.rsi14(closes(vals)), 1e-9);
    }

    @Test
    void rsi14_mixedGainsAndLosses_matchesFormula() {
        // 7 up days of +2, then 7 down days of -1, starting at 100
        double[] vals = new double[15];
        vals[0] = 100;
        for (int i = 1; i <= 7; i++) vals[i] = vals[i - 1] + 2;
        for (int i = 8; i <= 14; i++) vals[i] = vals[i - 1] - 1;
        double avgGain = (7 * 2.0) / 14;
        double avgLoss = (7 * 1.0) / 14;
        double expected = 100 - (100 / (1 + avgGain / avgLoss));
        assertEquals(expected, RiskMath.rsi14(closes(vals)), 1e-9);
    }

    @Test
    void rsi14_insufficientHistory_returnsNull() {
        assertNull(RiskMath.rsi14(closes(1, 2, 3)));
    }

    @Test
    void covarianceAndVariance_consistentWithBeta() {
        List<Double> x = List.of(0.01, 0.03, -0.02, 0.04);
        double var = RiskMath.variance(x);
        assertTrue(var > 0);
        assertEquals(RiskMath.covariance(x, x), var, 1e-9);
    }
}

