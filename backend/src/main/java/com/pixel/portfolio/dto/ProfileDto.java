package com.pixel.portfolio.dto;

public class ProfileDto {
    private String symbol;
    private String name;
    private String logo;
    private String exchange;
    private String currency;
    private String source; // LIVE or DB_FALLBACK

    public ProfileDto() {}

    public ProfileDto(String symbol, String name, String logo, String exchange, String currency, String source) {
        this.symbol = symbol;
        this.name = name;
        this.logo = logo;
        this.exchange = exchange;
        this.currency = currency;
        this.source = source;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}

