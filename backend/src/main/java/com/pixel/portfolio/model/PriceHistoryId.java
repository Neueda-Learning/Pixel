package com.pixel.portfolio.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class PriceHistoryId implements Serializable {
    private String symbol;
    private LocalDate tradeDate;

    public PriceHistoryId() {}

    public PriceHistoryId(String symbol, LocalDate tradeDate) {
        this.symbol = symbol;
        this.tradeDate = tradeDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceHistoryId)) return false;
        PriceHistoryId that = (PriceHistoryId) o;
        return Objects.equals(symbol, that.symbol) && Objects.equals(tradeDate, that.tradeDate);
    }

    @Override
    public int hashCode() { return Objects.hash(symbol, tradeDate); }
}
