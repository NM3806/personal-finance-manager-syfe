package com.finance.manager.service;

import com.finance.manager.dto.MonthlyReportResponse;
import com.finance.manager.dto.YearlyReportResponse;
import com.finance.manager.entity.Category;
import com.finance.manager.entity.CategoryType;
import com.finance.manager.entity.Transaction;
import com.finance.manager.entity.User;
import com.finance.manager.exception.BadRequestException;
import com.finance.manager.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ReportService reportService;

    private User testUser;
    private Category salaryCat;
    private Category freelanceCat;
    private Category foodCat;
    private Category rentCat;
    private Category transportCat;

    @BeforeEach
    void setUp() {
        testUser = new User("user@example.com", "hash", "User", "+1234567890");
        testUser.setId(1L);

        salaryCat = new Category("Salary", CategoryType.INCOME, null, false);
        freelanceCat = new Category("Freelance", CategoryType.INCOME, testUser, true);
        foodCat = new Category("Food", CategoryType.EXPENSE, null, false);
        rentCat = new Category("Rent", CategoryType.EXPENSE, null, false);
        transportCat = new Category("Transportation", CategoryType.EXPENSE, null, false);
    }

    @Test
    void getMonthlyReport_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);

        Transaction t1 = new Transaction(BigDecimal.valueOf(3000), LocalDate.of(2024, 1, 10), salaryCat, "Salary", CategoryType.INCOME, testUser);
        Transaction t2 = new Transaction(BigDecimal.valueOf(500), LocalDate.of(2024, 1, 12), freelanceCat, "Freelance", CategoryType.INCOME, testUser);
        Transaction t3 = new Transaction(BigDecimal.valueOf(400), LocalDate.of(2024, 1, 15), foodCat, "Food", CategoryType.EXPENSE, testUser);
        Transaction t4 = new Transaction(BigDecimal.valueOf(1200), LocalDate.of(2024, 1, 20), rentCat, "Rent", CategoryType.EXPENSE, testUser);
        Transaction t5 = new Transaction(BigDecimal.valueOf(200), LocalDate.of(2024, 1, 25), transportCat, "Transport", CategoryType.EXPENSE, testUser);

        when(transactionRepository.findByUserAndDateBetween(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(t1, t2, t3, t4, t5));

        MonthlyReportResponse report = reportService.getMonthlyReport(2024, 1);

        assertNotNull(report);
        assertEquals(1, report.getMonth());
        assertEquals(2024, report.getYear());
        assertEquals(BigDecimal.valueOf(3000).setScale(2), report.getTotalIncome().get("Salary"));
        assertEquals(BigDecimal.valueOf(500).setScale(2), report.getTotalIncome().get("Freelance"));
        assertEquals(BigDecimal.valueOf(400).setScale(2), report.getTotalExpenses().get("Food"));
        assertEquals(BigDecimal.valueOf(1200).setScale(2), report.getTotalExpenses().get("Rent"));
        assertEquals(BigDecimal.valueOf(200).setScale(2), report.getTotalExpenses().get("Transportation"));
        assertEquals(BigDecimal.valueOf(1700).setScale(2), report.getNetSavings());
    }

    @Test
    void getMonthlyReport_invalidMonth_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> reportService.getMonthlyReport(2024, 13));
        assertThrows(BadRequestException.class, () -> reportService.getMonthlyReport(2024, 0));
    }

    @Test
    void getMonthlyReport_invalidYear_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> reportService.getMonthlyReport(1800, 1));
        assertThrows(BadRequestException.class, () -> reportService.getMonthlyReport(2200, 1));
    }

    @Test
    void getYearlyReport_invalidYear_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> reportService.getYearlyReport(1800));
        assertThrows(BadRequestException.class, () -> reportService.getYearlyReport(2200));
    }

    @Test
    void getYearlyReport_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);

        Transaction t1 = new Transaction(BigDecimal.valueOf(36000), LocalDate.of(2024, 6, 1), salaryCat, "Salary", CategoryType.INCOME, testUser);
        Transaction t2 = new Transaction(BigDecimal.valueOf(6000), LocalDate.of(2024, 7, 1), freelanceCat, "Freelance", CategoryType.INCOME, testUser);
        Transaction t3 = new Transaction(BigDecimal.valueOf(4800), LocalDate.of(2024, 8, 1), foodCat, "Food", CategoryType.EXPENSE, testUser);
        Transaction t4 = new Transaction(BigDecimal.valueOf(14400), LocalDate.of(2024, 9, 1), rentCat, "Rent", CategoryType.EXPENSE, testUser);
        Transaction t5 = new Transaction(BigDecimal.valueOf(2400), LocalDate.of(2024, 10, 1), transportCat, "Transport", CategoryType.EXPENSE, testUser);

        when(transactionRepository.findByUserAndDateBetween(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(t1, t2, t3, t4, t5));

        YearlyReportResponse report = reportService.getYearlyReport(2024);

        assertNotNull(report);
        assertEquals(2024, report.getYear());
        assertEquals(BigDecimal.valueOf(36000).setScale(2), report.getTotalIncome().get("Salary"));
        assertEquals(BigDecimal.valueOf(6000).setScale(2), report.getTotalIncome().get("Freelance"));
        assertEquals(BigDecimal.valueOf(4800).setScale(2), report.getTotalExpenses().get("Food"));
        assertEquals(BigDecimal.valueOf(14400).setScale(2), report.getTotalExpenses().get("Rent"));
        assertEquals(BigDecimal.valueOf(2400).setScale(2), report.getTotalExpenses().get("Transportation"));
        assertEquals(BigDecimal.valueOf(20400).setScale(2), report.getNetSavings());
    }

    @Test
    void getYearlyReport_emptyTransactions_returnsZeroNetSavings() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(transactionRepository.findByUserAndDateBetween(eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        YearlyReportResponse report = reportService.getYearlyReport(2024);

        assertNotNull(report);
        assertEquals(2024, report.getYear());
        assertTrue(report.getTotalIncome().isEmpty());
        assertTrue(report.getTotalExpenses().isEmpty());
        assertEquals(BigDecimal.ZERO.setScale(2), report.getNetSavings());
    }
}
