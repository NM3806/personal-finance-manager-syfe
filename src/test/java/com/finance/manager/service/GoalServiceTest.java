package com.finance.manager.service;

import com.finance.manager.dto.CreateGoalRequest;
import com.finance.manager.dto.GoalListResponse;
import com.finance.manager.dto.GoalResponse;
import com.finance.manager.dto.MessageResponse;
import com.finance.manager.dto.UpdateGoalRequest;
import com.finance.manager.entity.Category;
import com.finance.manager.entity.CategoryType;
import com.finance.manager.entity.SavingsGoal;
import com.finance.manager.entity.Transaction;
import com.finance.manager.entity.User;
import com.finance.manager.exception.BadRequestException;
import com.finance.manager.exception.ForbiddenException;
import com.finance.manager.exception.ResourceNotFoundException;
import com.finance.manager.repository.SavingsGoalRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private SavingsGoalRepository savingsGoalRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthService authService;

    @InjectMocks
    private GoalService goalService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("user@example.com", "hash", "User", "+1234567890");
        testUser.setId(1L);
    }

    @Test
    void createGoal_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        LocalDate targetDate = LocalDate.now().plusMonths(6);
        LocalDate startDate = LocalDate.now().minusMonths(1);

        CreateGoalRequest request = new CreateGoalRequest("Emergency Fund", BigDecimal.valueOf(5000), targetDate, startDate);

        SavingsGoal savedGoal = new SavingsGoal("Emergency Fund", BigDecimal.valueOf(5000), targetDate, startDate, testUser);
        savedGoal.setId(1L);
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(savedGoal);

        Category salary = new Category("Salary", CategoryType.INCOME, null, false);
        Category rent = new Category("Rent", CategoryType.EXPENSE, null, false);
        Transaction t1 = new Transaction(BigDecimal.valueOf(3000), LocalDate.now(), salary, "Pay", CategoryType.INCOME, testUser);
        Transaction t2 = new Transaction(BigDecimal.valueOf(2000), LocalDate.now(), rent, "Rent", CategoryType.EXPENSE, testUser);

        when(transactionRepository.findByUserAndDateGreaterThanEqual(testUser, startDate)).thenReturn(List.of(t1, t2));

        GoalResponse response = goalService.createGoal(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Emergency Fund", response.getGoalName());
        assertEquals(BigDecimal.valueOf(5000).setScale(2), response.getTargetAmount());
        assertEquals(BigDecimal.valueOf(1000).setScale(2), response.getCurrentProgress());
        assertEquals(20.0, response.getProgressPercentage());
        assertEquals(BigDecimal.valueOf(4000).setScale(2), response.getRemainingAmount());
    }

    @Test
    void createGoal_pastTargetDate_throwsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        CreateGoalRequest request = new CreateGoalRequest("Past Goal", BigDecimal.valueOf(1000), LocalDate.now().minusDays(1), null);

        assertThrows(BadRequestException.class, () -> goalService.createGoal(request));
    }

    @Test
    void createGoal_nonPositiveTargetAmount_throwsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        CreateGoalRequest request = new CreateGoalRequest("Zero Goal", BigDecimal.ZERO, LocalDate.now().plusMonths(1), null);

        assertThrows(BadRequestException.class, () -> goalService.createGoal(request));
    }

    @Test
    void getAllGoals_returnsList() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        LocalDate targetDate = LocalDate.now().plusMonths(6);
        SavingsGoal g1 = new SavingsGoal("Trip", BigDecimal.valueOf(2000), targetDate, LocalDate.now(), testUser);
        g1.setId(1L);

        when(savingsGoalRepository.findByUserOrderByIdAsc(testUser)).thenReturn(List.of(g1));
        when(transactionRepository.findByUserAndDateGreaterThanEqual(testUser, g1.getStartDate())).thenReturn(List.of());

        GoalListResponse response = goalService.getAllGoals();

        assertNotNull(response);
        assertEquals(1, response.getGoals().size());
        assertEquals(BigDecimal.ZERO, response.getGoals().get(0).getCurrentProgress());
        assertEquals(BigDecimal.valueOf(2000).setScale(2), response.getGoals().get(0).getRemainingAmount());
    }

    @Test
    void getGoalById_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        LocalDate targetDate = LocalDate.now().plusMonths(6);
        SavingsGoal g1 = new SavingsGoal("Trip", BigDecimal.valueOf(2000), targetDate, LocalDate.now(), testUser);
        g1.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(g1));
        when(transactionRepository.findByUserAndDateGreaterThanEqual(testUser, g1.getStartDate())).thenReturn(List.of());

        GoalResponse response = goalService.getGoalById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void getGoalById_notFound_throwsNotFound() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(savingsGoalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> goalService.getGoalById(999L));
    }

    @Test
    void getGoalById_otherUser_throwsForbidden() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        User otherUser = new User("other@example.com", "hash", "Other", "+111");
        otherUser.setId(2L);

        SavingsGoal g1 = new SavingsGoal("Trip", BigDecimal.valueOf(2000), LocalDate.now().plusMonths(6), LocalDate.now(), otherUser);
        g1.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(g1));

        assertThrows(ForbiddenException.class, () -> goalService.getGoalById(1L));
    }

    @Test
    void updateGoal_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        SavingsGoal g1 = new SavingsGoal("Trip", BigDecimal.valueOf(2000), LocalDate.now().plusMonths(6), LocalDate.now(), testUser);
        g1.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(g1));
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(i -> i.getArgument(0));

        LocalDate newTargetDate = LocalDate.now().plusMonths(12);
        UpdateGoalRequest request = new UpdateGoalRequest("Updated Trip", BigDecimal.valueOf(3000), newTargetDate);

        when(transactionRepository.findByUserAndDateGreaterThanEqual(testUser, g1.getStartDate())).thenReturn(List.of());

        GoalResponse response = goalService.updateGoal(1L, request);

        assertNotNull(response);
        assertEquals("Updated Trip", response.getGoalName());
        assertEquals(BigDecimal.valueOf(3000).setScale(2), response.getTargetAmount());
        assertEquals(newTargetDate, response.getTargetDate());
    }

    @Test
    void updateGoal_pastDate_throwsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        SavingsGoal g1 = new SavingsGoal("Trip", BigDecimal.valueOf(2000), LocalDate.now().plusMonths(6), LocalDate.now(), testUser);
        g1.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(g1));

        UpdateGoalRequest request = new UpdateGoalRequest(BigDecimal.valueOf(3000), LocalDate.now().minusDays(1));
        assertThrows(BadRequestException.class, () -> goalService.updateGoal(1L, request));
    }

    @Test
    void deleteGoal_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        SavingsGoal g1 = new SavingsGoal("Trip", BigDecimal.valueOf(2000), LocalDate.now().plusMonths(6), LocalDate.now(), testUser);
        g1.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(g1));

        MessageResponse response = goalService.deleteGoal(1L);

        assertNotNull(response);
        assertEquals("Goal deleted successfully", response.getMessage());
        verify(savingsGoalRepository).delete(g1);
    }

    @Test
    void createGoal_nullStartDate_defaultsToNow() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        LocalDate targetDate = LocalDate.now().plusMonths(6);

        CreateGoalRequest request = new CreateGoalRequest("Trip", BigDecimal.valueOf(1000), targetDate, null);

        SavingsGoal savedGoal = new SavingsGoal("Trip", BigDecimal.valueOf(1000), targetDate, LocalDate.now(), testUser);
        savedGoal.setId(2L);
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenReturn(savedGoal);
        when(transactionRepository.findByUserAndDateGreaterThanEqual(eq(testUser), any(LocalDate.class))).thenReturn(List.of());

        GoalResponse response = goalService.createGoal(request);

        assertNotNull(response);
        assertEquals(LocalDate.now(), response.getStartDate());
    }

    @Test
    void createGoal_nullTargetDate_throwsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        CreateGoalRequest request = new CreateGoalRequest("Trip", BigDecimal.valueOf(1000), null, LocalDate.now());

        assertThrows(BadRequestException.class, () -> goalService.createGoal(request));
    }

    @Test
    void createGoal_nullTargetAmount_throwsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        CreateGoalRequest request = new CreateGoalRequest("Trip", null, LocalDate.now().plusMonths(6), LocalDate.now());

        assertThrows(BadRequestException.class, () -> goalService.createGoal(request));
    }

    @Test
    void createGoal_startDateAfterTargetDate_throwsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        LocalDate targetDate = LocalDate.now().plusMonths(1);
        LocalDate startDate = LocalDate.now().plusMonths(2);
        CreateGoalRequest request = new CreateGoalRequest("Trip", BigDecimal.valueOf(1000), targetDate, startDate);

        assertThrows(BadRequestException.class, () -> goalService.createGoal(request));
    }

    @Test
    void updateGoal_notFound_throwsNotFound() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(savingsGoalRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateGoalRequest request = new UpdateGoalRequest("Updated", BigDecimal.valueOf(2000), LocalDate.now().plusMonths(6));
        assertThrows(ResourceNotFoundException.class, () -> goalService.updateGoal(999L, request));
    }

    @Test
    void updateGoal_otherUser_throwsForbidden() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        User otherUser = new User("other@example.com", "hash", "Other", "+111");
        otherUser.setId(2L);

        SavingsGoal g1 = new SavingsGoal("Trip", BigDecimal.valueOf(2000), LocalDate.now().plusMonths(6), LocalDate.now(), otherUser);
        g1.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(g1));

        UpdateGoalRequest request = new UpdateGoalRequest("Updated", BigDecimal.valueOf(2000), LocalDate.now().plusMonths(6));
        assertThrows(ForbiddenException.class, () -> goalService.updateGoal(1L, request));
    }

    @Test
    void updateGoal_negativeTargetAmount_throwsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        SavingsGoal g1 = new SavingsGoal("Trip", BigDecimal.valueOf(2000), LocalDate.now().plusMonths(6), LocalDate.now(), testUser);
        g1.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(g1));

        UpdateGoalRequest request = new UpdateGoalRequest(BigDecimal.valueOf(-100), LocalDate.now().plusMonths(6));
        assertThrows(BadRequestException.class, () -> goalService.updateGoal(1L, request));
    }

    @Test
    void updateGoal_withNullFields_noChanges() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        SavingsGoal g1 = new SavingsGoal("Trip", BigDecimal.valueOf(2000), LocalDate.now().plusMonths(6), LocalDate.now(), testUser);
        g1.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(g1));
        when(savingsGoalRepository.save(any(SavingsGoal.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.findByUserAndDateGreaterThanEqual(testUser, g1.getStartDate())).thenReturn(List.of());

        UpdateGoalRequest request = new UpdateGoalRequest();

        GoalResponse response = goalService.updateGoal(1L, request);

        assertNotNull(response);
        assertEquals("Trip", response.getGoalName());
        assertEquals(BigDecimal.valueOf(2000).setScale(2), response.getTargetAmount());
    }

    @Test
    void deleteGoal_notFound_throwsNotFound() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(savingsGoalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> goalService.deleteGoal(999L));
    }

    @Test
    void deleteGoal_otherUser_throwsForbidden() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        User otherUser = new User("other@example.com", "hash", "Other", "+111");
        otherUser.setId(2L);

        SavingsGoal g1 = new SavingsGoal("Trip", BigDecimal.valueOf(2000), LocalDate.now().plusMonths(6), LocalDate.now(), otherUser);
        g1.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(g1));

        assertThrows(ForbiddenException.class, () -> goalService.deleteGoal(1L));
    }

    @Test
    void toGoalResponse_negativeProgress_and_progressExceedingTarget() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        LocalDate targetDate = LocalDate.now().plusMonths(6);
        SavingsGoal g1 = new SavingsGoal("Trip", BigDecimal.valueOf(1000), targetDate, LocalDate.now(), testUser);
        g1.setId(1L);

        when(savingsGoalRepository.findById(1L)).thenReturn(Optional.of(g1));

        Category expenseCat = new Category("Rent", CategoryType.EXPENSE, null, false);
        Transaction tExpense = new Transaction(BigDecimal.valueOf(1500), LocalDate.now(), expenseCat, "Rent", CategoryType.EXPENSE, testUser);

        when(transactionRepository.findByUserAndDateGreaterThanEqual(testUser, g1.getStartDate())).thenReturn(List.of(tExpense));

        GoalResponse responseNeg = goalService.getGoalById(1L);
        assertEquals(0.0, responseNeg.getProgressPercentage());
        assertEquals(BigDecimal.valueOf(-1500).setScale(2), responseNeg.getCurrentProgress());

        Category incomeCat = new Category("Salary", CategoryType.INCOME, null, false);
        Transaction tBigIncome = new Transaction(BigDecimal.valueOf(5000), LocalDate.now(), incomeCat, "Bonus", CategoryType.INCOME, testUser);
        when(transactionRepository.findByUserAndDateGreaterThanEqual(testUser, g1.getStartDate())).thenReturn(List.of(tBigIncome));

        GoalResponse responseExceed = goalService.getGoalById(1L);
        assertEquals(BigDecimal.valueOf(0).setScale(2), responseExceed.getRemainingAmount());
        assertEquals(500.0, responseExceed.getProgressPercentage());
    }
}
