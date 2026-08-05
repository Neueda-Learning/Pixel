package com.pixel.portfolio.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public class TransactionRequestDto {

    @NotBlank(message = "symbol is required")
    private String symbol;

    @NotBlank(message = "txType is required")
    @Pattern(regexp = "(?i)BUY|SELL", message = "txType must be BUY or SELL")
    private String txType;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be positive")
    private BigDecimal quantity;

    @NotNull(message = "price is required")
    @Positive(message = "price must be positive")
    private BigDecimal price;

    @PositiveOrZero(message = "fees cannot be negative")
    private BigDecimal fees = BigDecimal.ZERO;

    @NotNull(message = "executedAt is required")
    private Instant executedAt;

    @Size(max = 500, message = "notes must be at most 500 characters")
    private String notes;

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
