package com.pixel.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** An open (not fully sold) buy lot, used to power the Sell form's buy-price picker. */
public class LotDto {
    private Long transactionId;
    private BigDecimal price;
    private Instant executedAt;
    private BigDecimal remainingQuantity;

    public LotDto() {}

    public LotDto(Long transactionId, BigDecimal price, Instant executedAt, BigDecimal remainingQuantity) {
        this.transactionId = transactionId;
        this.price = price;
        this.executedAt = executedAt;
        this.remainingQuantity = remainingQuantity;
    }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
    public BigDecimal getRemainingQuantity() { return remainingQuantity; }
    public void setRemainingQuantity(BigDecimal remainingQuantity) { this.remainingQuantity = remainingQuantity; }
}
