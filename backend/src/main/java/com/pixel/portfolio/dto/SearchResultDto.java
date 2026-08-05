package com.pixel.portfolio.dto;

public class SearchResultDto {
    private String symbol;
    private String description;
    private String type;

    public SearchResultDto() {}

    public SearchResultDto(String symbol, String description, String type) {
        this.symbol = symbol;
        this.description = description;
        this.type = type;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}

