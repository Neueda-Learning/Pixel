package com.pixel.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PerformancePointDto {
    private LocalDate date;
    private BigDecimal value;

    public PerformancePointDto() {}

    public PerformancePointDto(LocalDate date, BigDecimal value) {
        this.date = date;
        this.value = value;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
}
