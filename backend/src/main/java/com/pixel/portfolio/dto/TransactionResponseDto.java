package com.pixel.portfolio.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionResponseDto {
    private Long id;
    private String symbol;
    private String txType;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal fees;
    private Instant executedAt;
    private String notes;

    public TransactionResponseDto() {}

    public TransactionResponseDto(Long id, String symbol, String txType, BigDecimal quantity, BigDecimal price,
                                   BigDecimal fees, Instant executedAt, String notes) {
        this.id = id;
        this.symbol = symbol;
        this.txType = txType;
        this.quantity = quantity;
        this.price = price;
        this.fees = fees;
        this.executedAt = executedAt;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getTxType() { return txType; }
    public void setTxType(String txType) { this.txType = txType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal fees) { this.fees = fees; }
    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant executedAt) { this.executedAt = executedAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
