package com.pixel.portfolio.dto;

import java.math.BigDecimal;

public class HoldingDto {
    private String symbol;
    private String name;
    private String assetType;
    private BigDecimal quantity;
    private BigDecimal avgCost;
    private BigDecimal currentPrice;
    private BigDecimal marketValue;
    private BigDecimal gainLoss;
    private BigDecimal gainLossPct;
    private String priceSource; // LIVE or DB_FALLBACK

    public HoldingDto() {}

    public HoldingDto(String symbol, String name, String assetType, BigDecimal quantity, BigDecimal avgCost,
                       BigDecimal currentPrice, BigDecimal marketValue, BigDecimal gainLoss, BigDecimal gainLossPct,
                       String priceSource) {
        this.symbol = symbol;
        this.name = name;
        this.assetType = assetType;
        this.quantity = quantity;
        this.avgCost = avgCost;
        this.currentPrice = currentPrice;
        this.marketValue = marketValue;
        this.gainLoss = gainLoss;
        this.gainLossPct = gainLossPct;
        this.priceSource = priceSource;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAvgCost() { return avgCost; }
    public void setAvgCost(BigDecimal avgCost) { this.avgCost = avgCost; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getMarketValue() { return marketValue; }
    public void setMarketValue(BigDecimal marketValue) { this.marketValue = marketValue; }
    public BigDecimal getGainLoss() { return gainLoss; }
    public void setGainLoss(BigDecimal gainLoss) { this.gainLoss = gainLoss; }
    public BigDecimal getGainLossPct() { return gainLossPct; }
    public void setGainLossPct(BigDecimal gainLossPct) { this.gainLossPct = gainLossPct; }
    public String getPriceSource() { return priceSource; }
    public void setPriceSource(String priceSource) { this.priceSource = priceSource; }
}
