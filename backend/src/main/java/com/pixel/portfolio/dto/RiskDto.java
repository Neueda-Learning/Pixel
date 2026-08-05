package com.pixel.portfolio.dto;

import java.time.LocalDate;

public class RiskDto {
    private String symbol;
    private LocalDate asOf;
    private int dataPoints;
    private Double annualizedVolatility;
    private Double annualizedReturn;
    private Double sharpeRatio;
    private Double maxDrawdown;
    private Double beta;
    private Double sma50;
    private Double sma200;
    private String trend; // BULLISH, BEARISH, NEUTRAL, UNKNOWN
    private Double rsi14;
    private String recommendation; // BUY, HOLD, AVOID
    private String rationale;
    private String disclaimer = "Educational only, not financial advice.";

    public RiskDto() {}

    public RiskDto(String symbol, LocalDate asOf, int dataPoints, Double annualizedVolatility, Double annualizedReturn,
                    Double sharpeRatio, Double maxDrawdown, Double beta, Double sma50, Double sma200, String trend,
                    Double rsi14, String recommendation, String rationale) {
        this.symbol = symbol;
        this.asOf = asOf;
        this.dataPoints = dataPoints;
        this.annualizedVolatility = annualizedVolatility;
        this.annualizedReturn = annualizedReturn;
        this.sharpeRatio = sharpeRatio;
        this.maxDrawdown = maxDrawdown;
        this.beta = beta;
        this.sma50 = sma50;
        this.sma200 = sma200;
        this.trend = trend;
        this.rsi14 = rsi14;
        this.recommendation = recommendation;
        this.rationale = rationale;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
    public int getDataPoints() { return dataPoints; }
    public void setDataPoints(int dataPoints) { this.dataPoints = dataPoints; }
    public Double getAnnualizedVolatility() { return annualizedVolatility; }
    public void setAnnualizedVolatility(Double annualizedVolatility) { this.annualizedVolatility = annualizedVolatility; }
    public Double getAnnualizedReturn() { return annualizedReturn; }
    public void setAnnualizedReturn(Double annualizedReturn) { this.annualizedReturn = annualizedReturn; }
    public Double getSharpeRatio() { return sharpeRatio; }
    public void setSharpeRatio(Double sharpeRatio) { this.sharpeRatio = sharpeRatio; }
    public Double getMaxDrawdown() { return maxDrawdown; }
    public void setMaxDrawdown(Double maxDrawdown) { this.maxDrawdown = maxDrawdown; }
    public Double getBeta() { return beta; }
    public void setBeta(Double beta) { this.beta = beta; }
    public Double getSma50() { return sma50; }
    public void setSma50(Double sma50) { this.sma50 = sma50; }
    public Double getSma200() { return sma200; }
    public void setSma200(Double sma200) { this.sma200 = sma200; }
    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }
    public Double getRsi14() { return rsi14; }
    public void setRsi14(Double rsi14) { this.rsi14 = rsi14; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
}

