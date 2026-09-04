package com.finance.manager.service;

import com.finance.manager.dto.CreateTransactionRequest;
import com.finance.manager.dto.MessageResponse;
import com.finance.manager.dto.TransactionListResponse;
import com.finance.manager.dto.TransactionResponse;
import com.finance.manager.dto.UpdateTransactionRequest;
import com.finance.manager.entity.Category;
import com.finance.manager.entity.CategoryType;
import com.finance.manager.entity.Transaction;
import com.finance.manager.entity.User;
import com.finance.manager.exception.BadRequestException;
import com.finance.manager.exception.ForbiddenException;
import com.finance.manager.exception.ResourceNotFoundException;
import com.finance.manager.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Category salaryCategory;

    @BeforeEach
    void setUp() {
        testUser = new User("user@example.com", "hash", "User", "+1234567890");
        testUser.setId(1L);

        salaryCategory = new Category("Salary", CategoryType.INCOME, null, false);
        salaryCategory.setId(10L);
    }

    @Test
    void createTransaction_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        LocalDate date = LocalDate.now().minusDays(1);
        CreateTransactionRequest request = new CreateTransactionRequest(BigDecimal.valueOf(5000), date, "Salary", "Bonus");

        when(categoryService.getValidCategory("Salary", testUser)).thenReturn(salaryCategory);

        Transaction savedTransaction = new Transaction(BigDecimal.valueOf(5000), date, salaryCategory, "Bonus", CategoryType.INCOME, testUser);
        savedTransaction.setId(100L);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        TransactionResponse response = transactionService.createTransaction(request);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(BigDecimal.valueOf(5000).setScale(2), response.getAmount());
        assertEquals("Salary", response.getCategory());
        assertEquals(CategoryType.INCOME, response.getType());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void createTransaction_futureDate_throwsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        LocalDate futureDate = LocalDate.now().plusDays(2);
        CreateTransactionRequest request = new CreateTransactionRequest(BigDecimal.valueOf(100), futureDate, "Salary", "Future");

        assertThrows(BadRequestException.class, () -> transactionService.createTransaction(request));
    }

    @Test
    void getTransactions_returnsList() {
        when(authService.getCurrentUser()).thenReturn(testUser);

        Transaction t1 = new Transaction(BigDecimal.valueOf(5000), LocalDate.now(), salaryCategory, "Salary", CategoryType.INCOME, testUser);
        t1.setId(1L);

        when(transactionRepository.findAll(any(Specification.class))).thenReturn(List.of(t1));

        TransactionListResponse response = transactionService.getTransactions(null, null, null, null, null);

        assertNotNull(response);
        assertEquals(1, response.getTransactions().size());
        assertEquals(1L, response.getTransactions().get(0).getId());
    }

    @Test
    void updateTransaction_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        LocalDate date = LocalDate.now().minusDays(1);
        Transaction existing = new Transaction(BigDecimal.valueOf(5000), date, salaryCategory, "Old", CategoryType.INCOME, testUser);
        existing.setId(1L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        Category foodCategory = new Category("Food", CategoryType.EXPENSE, null, false);
        when(categoryService.getValidCategory("Food", testUser)).thenReturn(foodCategory);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        UpdateTransactionRequest request = new UpdateTransactionRequest(BigDecimal.valueOf(6000), "Food", "Updated");

        TransactionResponse response = transactionService.updateTransaction(1L, request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(6000).setScale(2), response.getAmount());
        assertEquals("Food", response.getCategory());
        assertEquals(CategoryType.EXPENSE, response.getType());
        assertEquals("Updated", response.getDescription());
    }

    @Test
    void updateTransaction_dateModified_throwsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        LocalDate date = LocalDate.now().minusDays(1);
        Transaction existing = new Transaction(BigDecimal.valueOf(5000), date, salaryCategory, "Old", CategoryType.INCOME, testUser);
        existing.setId(1L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        UpdateTransactionRequest request = new UpdateTransactionRequest(BigDecimal.valueOf(6000), "Salary", "Updated");
        request.setDate(LocalDate.now().minusDays(5));

        assertThrows(BadRequestException.class, () -> transactionService.updateTransaction(1L, request));
    }

    @Test
    void updateTransaction_notFound_throwsNotFound() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateTransactionRequest request = new UpdateTransactionRequest(BigDecimal.valueOf(100), "Desc");
        assertThrows(ResourceNotFoundException.class, () -> transactionService.updateTransaction(999L, request));
    }

    @Test
    void updateTransaction_otherUser_throwsForbidden() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        User otherUser = new User("other@example.com", "hash", "Other", "+111");
        otherUser.setId(2L);

        Transaction existing = new Transaction(BigDecimal.valueOf(5000), LocalDate.now(), salaryCategory, "Old", CategoryType.INCOME, otherUser);
        existing.setId(1L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        UpdateTransactionRequest request = new UpdateTransactionRequest(BigDecimal.valueOf(100), "Desc");
        assertThrows(ForbiddenException.class, () -> transactionService.updateTransaction(1L, request));
    }

    @Test
    void deleteTransaction_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        Transaction existing = new Transaction(BigDecimal.valueOf(5000), LocalDate.now(), salaryCategory, "Old", CategoryType.INCOME, testUser);
        existing.setId(1L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        MessageResponse response = transactionService.deleteTransaction(1L);

        assertNotNull(response);
        assertEquals("Transaction deleted successfully", response.getMessage());
        verify(transactionRepository).delete(existing);
    }

    @Test
    void deleteTransaction_notFound_throwsNotFound() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> transactionService.deleteTransaction(999L));
    }

    @Test
    void deleteTransaction_otherUser_throwsForbidden() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        User otherUser = new User("other@example.com", "hash", "Other", "+111");
        otherUser.setId(2L);

        Transaction existing = new Transaction(BigDecimal.valueOf(5000), LocalDate.now(), salaryCategory, "Old", CategoryType.INCOME, otherUser);
        existing.setId(1L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(ForbiddenException.class, () -> transactionService.deleteTransaction(1L));
    }

    @Test
    void getTransactions_withAllFilters() {
        when(authService.getCurrentUser()).thenReturn(testUser);

        LocalDate start = LocalDate.now().minusMonths(1);
        LocalDate end = LocalDate.now();

        Transaction t1 = new Transaction(BigDecimal.valueOf(5000), LocalDate.now(), salaryCategory, "Salary", CategoryType.INCOME, testUser);
        t1.setId(1L);

        when(transactionRepository.findAll(any(Specification.class))).thenReturn(List.of(t1));

        TransactionListResponse response = transactionService.getTransactions(start, end, 10L, "Salary", CategoryType.INCOME);

        assertNotNull(response);
        assertEquals(1, response.getTransactions().size());
    }

    @Test
    void updateTransaction_withSameDateAndNullFields() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        LocalDate date = LocalDate.now().minusDays(1);
        Transaction existing = new Transaction(BigDecimal.valueOf(5000), date, salaryCategory, "Old", CategoryType.INCOME, testUser);
        existing.setId(1L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        UpdateTransactionRequest request = new UpdateTransactionRequest();
        request.setDate(date);

        TransactionResponse response = transactionService.updateTransaction(1L, request);

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(5000).setScale(2), response.getAmount());
        assertEquals("Old", response.getDescription());
        assertEquals("Salary", response.getCategory());
    }
}
