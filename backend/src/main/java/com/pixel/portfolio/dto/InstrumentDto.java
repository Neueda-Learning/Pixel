package com.pixel.portfolio.dto;

public class InstrumentDto {
    private String symbol;
    private String name;
    private String assetType;
    private String currency;

    public InstrumentDto() {}

    public InstrumentDto(String symbol, String name, String assetType, String currency) {
        this.symbol = symbol;
        this.name = name;
        this.assetType = assetType;
        this.currency = currency;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
