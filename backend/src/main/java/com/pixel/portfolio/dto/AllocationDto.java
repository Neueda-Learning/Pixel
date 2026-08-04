package com.pixel.portfolio.dto;

import java.math.BigDecimal;

public class AllocationDto {
    private String assetType;
    private BigDecimal value;
    private BigDecimal percentage;

    public AllocationDto() {}

    public AllocationDto(String assetType, BigDecimal value, BigDecimal percentage) {
        this.assetType = assetType;
        this.value = value;
        this.percentage = percentage;
    }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public BigDecimal getPercentage() { return percentage; }
    public void setPercentage(BigDecimal percentage) { this.percentage = percentage; }
}
