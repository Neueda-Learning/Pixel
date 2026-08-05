package com.pixel.portfolio.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_history")
@IdClass(PriceHistoryId.class)
public class PriceHistory {
    @Id
    private String symbol;

    @Id
    @Column(name = "trade_date")
    private LocalDate tradeDate;

    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;

    @Column(name = "adj_close")
    private BigDecimal adjClose;

    private Long volume;

    public PriceHistory() {}

    public PriceHistory(String symbol, LocalDate tradeDate, BigDecimal open, BigDecimal high,
                         BigDecimal low, BigDecimal close, BigDecimal adjClose, Long volume) {
        this.symbol = symbol;
        this.tradeDate = tradeDate;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.adjClose = adjClose;
        this.volume = volume;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public LocalDate getTradeDate() { return tradeDate; }
    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }
    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }
    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }
    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }
    public BigDecimal getAdjClose() { return adjClose; }
    public void setAdjClose(BigDecimal adjClose) { this.adjClose = adjClose; }
    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }
}
