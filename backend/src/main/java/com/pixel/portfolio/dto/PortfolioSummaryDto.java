package com.pixel.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

public class PortfolioSummaryDto {
    private BigDecimal totalValue;
    private BigDecimal totalCost;
    private BigDecimal totalGainLoss;
    private BigDecimal totalGainLossPct;
    private int holdingsCount;
    private List<AllocationDto> allocation;

    public PortfolioSummaryDto() {}

    public PortfolioSummaryDto(BigDecimal totalValue, BigDecimal totalCost, BigDecimal totalGainLoss,
                                BigDecimal totalGainLossPct, int holdingsCount, List<AllocationDto> allocation) {
        this.totalValue = totalValue;
        this.totalCost = totalCost;
        this.totalGainLoss = totalGainLoss;
        this.totalGainLossPct = totalGainLossPct;
        this.holdingsCount = holdingsCount;
        this.allocation = allocation;
    }

    public BigDecimal getTotalValue() { return totalValue; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public BigDecimal getTotalGainLoss() { return totalGainLoss; }
    public void setTotalGainLoss(BigDecimal totalGainLoss) { this.totalGainLoss = totalGainLoss; }
    public BigDecimal getTotalGainLossPct() { return totalGainLossPct; }
    public void setTotalGainLossPct(BigDecimal totalGainLossPct) { this.totalGainLossPct = totalGainLossPct; }
    public int getHoldingsCount() { return holdingsCount; }
    public void setHoldingsCount(int holdingsCount) { this.holdingsCount = holdingsCount; }
    public List<AllocationDto> getAllocation() { return allocation; }
    public void setAllocation(List<AllocationDto> allocation) { this.allocation = allocation; }
}
