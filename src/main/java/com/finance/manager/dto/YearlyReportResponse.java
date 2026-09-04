package com.finance.manager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class YearlyReportResponse {

    private int year;
    private Map<String, BigDecimal> totalIncome;
    private Map<String, BigDecimal> totalExpenses;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
    private BigDecimal netSavings;

    public YearlyReportResponse() {
    }

    public YearlyReportResponse(int year, Map<String, BigDecimal> totalIncome, Map<String, BigDecimal> totalExpenses, BigDecimal netSavings) {
        this.year = year;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.netSavings = netSavings != null ? (netSavings.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : netSavings.setScale(2, RoundingMode.HALF_UP)) : null;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Map<String, BigDecimal> getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(Map<String, BigDecimal> totalIncome) {
        this.totalIncome = totalIncome;
    }

    public Map<String, BigDecimal> getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(Map<String, BigDecimal> totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getNetSavings() {
        return netSavings;
    }

    public void setNetSavings(BigDecimal netSavings) {
        this.netSavings = netSavings != null ? (netSavings.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : netSavings.setScale(2, RoundingMode.HALF_UP)) : null;
    }
}
