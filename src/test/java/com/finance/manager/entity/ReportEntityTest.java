package com.finance.manager.entity;

import com.finance.manager.dto.MonthlyReportResponse;
import com.finance.manager.dto.YearlyReportResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportEntityTest {

    @Test
    void testMonthlyReportResponse() {
        MonthlyReportResponse response = new MonthlyReportResponse();
        response.setMonth(5);
        response.setYear(2024);
        response.setTotalIncome(Map.of("Salary", BigDecimal.valueOf(3000)));
        response.setTotalExpenses(Map.of("Rent", BigDecimal.valueOf(1200)));
        response.setNetSavings(BigDecimal.valueOf(1800));

        assertEquals(5, response.getMonth());
        assertEquals(2024, response.getYear());
        assertEquals(BigDecimal.valueOf(3000), response.getTotalIncome().get("Salary"));
        assertEquals(BigDecimal.valueOf(1200), response.getTotalExpenses().get("Rent"));
        assertEquals(BigDecimal.valueOf(1800).setScale(2), response.getNetSavings());
    }

    @Test
    void testYearlyReportResponse() {
        YearlyReportResponse response = new YearlyReportResponse();
        response.setYear(2024);
        response.setTotalIncome(Map.of("Salary", BigDecimal.valueOf(36000)));
        response.setTotalExpenses(Map.of("Rent", BigDecimal.valueOf(14400)));
        response.setNetSavings(BigDecimal.valueOf(21600));

        assertEquals(2024, response.getYear());
        assertEquals(BigDecimal.valueOf(36000), response.getTotalIncome().get("Salary"));
        assertEquals(BigDecimal.valueOf(14400), response.getTotalExpenses().get("Rent"));
        assertEquals(BigDecimal.valueOf(21600).setScale(2), response.getNetSavings());
    }
}
