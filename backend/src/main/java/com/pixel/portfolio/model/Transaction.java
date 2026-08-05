package com.pixel.portfolio.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction")
public class Transaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String symbol;
    private String txType;          // BUY / SELL
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal fees = BigDecimal.ZERO;
    private Instant executedAt = Instant.now();
    private String notes;

    // TODO: getters & setters (or switch to a Java record / Lombok)
    public Long getId() { return id; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String s) { this.symbol = s; }
    public String getTxType() { return txType; }
    public void setTxType(String t) { this.txType = t; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal q) { this.quantity = q; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal p) { this.price = p; }
    public BigDecimal getFees() { return fees; }
    public void setFees(BigDecimal f) { this.fees = f; }
    public Instant getExecutedAt() { return executedAt; }
    public void setExecutedAt(Instant e) { this.executedAt = e; }
    public String getNotes() { return notes; }
    public void setNotes(String n) { this.notes = n; }
}
