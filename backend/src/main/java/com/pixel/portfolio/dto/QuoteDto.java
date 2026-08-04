package com.pixel.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class QuoteDto {
    private String symbol;
    private BigDecimal current;
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal open;
    private BigDecimal previousClose;
    private String source; // LIVE or DB_FALLBACK
    private Instant asOf;

    public QuoteDto() {}

    public QuoteDto(String symbol, BigDecimal current, BigDecimal change, BigDecimal changePercent,
                     BigDecimal high, BigDecimal low, BigDecimal open, BigDecimal previousClose,
                     String source, Instant asOf) {
        this.symbol = symbol;
        this.current = current;
        this.change = change;
        this.changePercent = changePercent;
        this.high = high;
        this.low = low;
        this.open = open;
        this.previousClose = previousClose;
        this.source = source;
        this.asOf = asOf;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public BigDecimal getCurrent() { return current; }
    public void setCurrent(BigDecimal current) { this.current = current; }
    public BigDecimal getChange() { return change; }
    public void setChange(BigDecimal change) { this.change = change; }
    public BigDecimal getChangePercent() { return changePercent; }
    public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    public BigDecimal getPreviousClose() { return previousClose; }
    public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getAsOf() { return asOf; }
    public void setAsOf(Instant asOf) { this.asOf = asOf; }
}

