package com.finance.manager.service;

import com.finance.manager.dto.MonthlyReportResponse;
import com.finance.manager.dto.YearlyReportResponse;
import com.finance.manager.entity.CategoryType;
import com.finance.manager.entity.Transaction;
import com.finance.manager.entity.User;
import com.finance.manager.exception.BadRequestException;
import com.finance.manager.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final AuthService authService;

    public ReportService(TransactionRepository transactionRepository, AuthService authService) {
        this.transactionRepository = transactionRepository;
        this.authService = authService;
    }

    public MonthlyReportResponse getMonthlyReport(int year, int month) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Invalid month: " + month + ". Month must be between 1 and 12.");
        }
        if (year < 1900 || year > 2100) {
            throw new BadRequestException("Invalid year: " + year);
        }

        User user = authService.getCurrentUser();

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(user, startDate, endDate);

        Map<String, BigDecimal> totalIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> totalExpenses = new LinkedHashMap<>();
        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            String categoryName = t.getCategory().getName();
            BigDecimal amount = t.getAmount().setScale(2, RoundingMode.HALF_UP);

            if (t.getType() == CategoryType.INCOME) {
                totalIncome.merge(categoryName, amount, BigDecimal::add);
                sumIncome = sumIncome.add(amount);
            } else if (t.getType() == CategoryType.EXPENSE) {
                totalExpenses.merge(categoryName, amount, BigDecimal::add);
                sumExpenses = sumExpenses.add(amount);
            }
        }

        BigDecimal netSavings = sumIncome.subtract(sumExpenses).setScale(2, RoundingMode.HALF_UP);
        return new MonthlyReportResponse(month, year, totalIncome, totalExpenses, netSavings);
    }

    public YearlyReportResponse getYearlyReport(int year) {
        if (year < 1900 || year > 2100) {
            throw new BadRequestException("Invalid year: " + year);
        }

        User user = authService.getCurrentUser();

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        List<Transaction> transactions = transactionRepository.findByUserAndDateBetween(user, startDate, endDate);

        Map<String, BigDecimal> totalIncome = new LinkedHashMap<>();
        Map<String, BigDecimal> totalExpenses = new LinkedHashMap<>();
        BigDecimal sumIncome = BigDecimal.ZERO;
        BigDecimal sumExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            String categoryName = t.getCategory().getName();
            BigDecimal amount = t.getAmount().setScale(2, RoundingMode.HALF_UP);

            if (t.getType() == CategoryType.INCOME) {
                totalIncome.merge(categoryName, amount, BigDecimal::add);
                sumIncome = sumIncome.add(amount);
            } else if (t.getType() == CategoryType.EXPENSE) {
                totalExpenses.merge(categoryName, amount, BigDecimal::add);
                sumExpenses = sumExpenses.add(amount);
            }
        }

        BigDecimal netSavings = sumIncome.subtract(sumExpenses).setScale(2, RoundingMode.HALF_UP);
        return new YearlyReportResponse(year, totalIncome, totalExpenses, netSavings);
    }
}
