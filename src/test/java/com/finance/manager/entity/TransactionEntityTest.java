package com.finance.manager.entity;

import com.finance.manager.dto.CreateTransactionRequest;
import com.finance.manager.dto.TransactionListResponse;
import com.finance.manager.dto.TransactionResponse;
import com.finance.manager.dto.UpdateTransactionRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TransactionEntityTest {

    @Test
    void testTransactionEntityGettersAndSetters() {
        Transaction transaction = new Transaction();
        transaction.setId(10L);
        transaction.setAmount(BigDecimal.valueOf(1500));
        LocalDate date = LocalDate.of(2024, 1, 15);
        transaction.setDate(date);

        Category category = new Category("Salary", CategoryType.INCOME, null, false);
        transaction.setCategory(category);
        transaction.setDescription("Salary payment");
        transaction.setType(CategoryType.INCOME);

        User user = new User();
        user.setId(1L);
        transaction.setUser(user);
        LocalDateTime now = LocalDateTime.now();
        transaction.setCreatedAt(now);

        assertEquals(10L, transaction.getId());
        assertEquals(BigDecimal.valueOf(1500), transaction.getAmount());
        assertEquals(date, transaction.getDate());
        assertEquals(category, transaction.getCategory());
        assertEquals("Salary payment", transaction.getDescription());
        assertEquals(CategoryType.INCOME, transaction.getType());
        assertEquals(user, transaction.getUser());
        assertEquals(now, transaction.getCreatedAt());
    }

    @Test
    void testTransactionDtos() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        CreateTransactionRequest createReq = new CreateTransactionRequest();
        createReq.setAmount(BigDecimal.valueOf(100));
        createReq.setDate(date);
        createReq.setCategory("Food");
        createReq.setDescription("Groceries");

        assertEquals(BigDecimal.valueOf(100), createReq.getAmount());
        assertEquals(date, createReq.getDate());
        assertEquals("Food", createReq.getCategory());
        assertEquals("Groceries", createReq.getDescription());

        UpdateTransactionRequest updateReq = new UpdateTransactionRequest();
        updateReq.setAmount(BigDecimal.valueOf(200));
        updateReq.setCategory("Food");
        updateReq.setDescription("Supermarket");
        updateReq.setDate(date);

        assertEquals(BigDecimal.valueOf(200), updateReq.getAmount());
        assertEquals("Food", updateReq.getCategory());
        assertEquals("Supermarket", updateReq.getDescription());
        assertEquals(date, updateReq.getDate());

        TransactionResponse response = new TransactionResponse();
        response.setId(1L);
        response.setAmount(BigDecimal.valueOf(500));
        response.setDate(date);
        response.setCategory("Salary");
        response.setDescription("Pay");
        response.setType(CategoryType.INCOME);

        assertEquals(1L, response.getId());
        assertEquals(BigDecimal.valueOf(500).setScale(2), response.getAmount());
        assertEquals(date, response.getDate());
        assertEquals("Salary", response.getCategory());
        assertEquals("Pay", response.getDescription());
        assertEquals(CategoryType.INCOME, response.getType());

        TransactionListResponse listResponse = new TransactionListResponse();
        listResponse.setTransactions(List.of(response));
        assertEquals(1, listResponse.getTransactions().size());
    }
}
