package com.finance.manager.service;

import com.finance.manager.dto.CreateGoalRequest;
import com.finance.manager.dto.GoalListResponse;
import com.finance.manager.dto.GoalResponse;
import com.finance.manager.dto.MessageResponse;
import com.finance.manager.dto.UpdateGoalRequest;
import com.finance.manager.entity.CategoryType;
import com.finance.manager.entity.SavingsGoal;
import com.finance.manager.entity.Transaction;
import com.finance.manager.entity.User;
import com.finance.manager.exception.BadRequestException;
import com.finance.manager.exception.ForbiddenException;
import com.finance.manager.exception.ResourceNotFoundException;
import com.finance.manager.repository.SavingsGoalRepository;
import com.finance.manager.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class GoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;
    private final AuthService authService;

    public GoalService(SavingsGoalRepository savingsGoalRepository,
                       TransactionRepository transactionRepository,
                       AuthService authService) {
        this.savingsGoalRepository = savingsGoalRepository;
        this.transactionRepository = transactionRepository;
        this.authService = authService;
    }

    @Transactional
    public GoalResponse createGoal(CreateGoalRequest request) {
        User user = authService.getCurrentUser();

        if (request.getTargetDate() == null || !request.getTargetDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("Target date must be a future date");
        }

        if (request.getTargetAmount() == null || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Target amount must be a positive decimal value");
        }

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();

        if (startDate.isAfter(request.getTargetDate())) {
            throw new BadRequestException("Start date cannot be after target date");
        }

        SavingsGoal goal = new SavingsGoal(
                request.getGoalName().trim(),
                request.getTargetAmount(),
                request.getTargetDate(),
                startDate,
                user
        );

        SavingsGoal saved = savingsGoalRepository.save(goal);
        return toGoalResponse(saved, user);
    }

    public GoalListResponse getAllGoals() {
        User user = authService.getCurrentUser();
        List<SavingsGoal> goals = savingsGoalRepository.findByUserOrderByIdAsc(user);

        List<GoalResponse> responses = goals.stream()
                .map(goal -> toGoalResponse(goal, user))
                .toList();

        return new GoalListResponse(responses);
    }

    public GoalResponse getGoalById(Long id) {
        User user = authService.getCurrentUser();

        Optional<SavingsGoal> optionalGoal = savingsGoalRepository.findById(id);
        if (optionalGoal.isEmpty()) {
            throw new ResourceNotFoundException("Goal not found: " + id);
        }

        SavingsGoal goal = optionalGoal.get();
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot access another user's goal");
        }

        return toGoalResponse(goal, user);
    }

    @Transactional
    public GoalResponse updateGoal(Long id, UpdateGoalRequest request) {
        User user = authService.getCurrentUser();

        Optional<SavingsGoal> optionalGoal = savingsGoalRepository.findById(id);
        if (optionalGoal.isEmpty()) {
            throw new ResourceNotFoundException("Goal not found: " + id);
        }

        SavingsGoal goal = optionalGoal.get();
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot update another user's goal");
        }

        if (request.getTargetDate() != null) {
            if (!request.getTargetDate().isAfter(LocalDate.now())) {
                throw new BadRequestException("Target date must be a future date");
            }
            goal.setTargetDate(request.getTargetDate());
        }

        if (request.getTargetAmount() != null) {
            if (request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Target amount must be a positive decimal value");
            }
            goal.setTargetAmount(request.getTargetAmount());
        }

        if (request.getGoalName() != null && !request.getGoalName().isBlank()) {
            goal.setGoalName(request.getGoalName().trim());
        }

        SavingsGoal updated = savingsGoalRepository.save(goal);
        return toGoalResponse(updated, user);
    }

    @Transactional
    public MessageResponse deleteGoal(Long id) {
        User user = authService.getCurrentUser();

        Optional<SavingsGoal> optionalGoal = savingsGoalRepository.findById(id);
        if (optionalGoal.isEmpty()) {
            throw new ResourceNotFoundException("Goal not found: " + id);
        }

        SavingsGoal goal = optionalGoal.get();
        if (!goal.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot delete another user's goal");
        }

        savingsGoalRepository.delete(goal);
        return new MessageResponse("Goal deleted successfully");
    }

    private GoalResponse toGoalResponse(SavingsGoal goal, User user) {
        List<Transaction> transactions = transactionRepository.findByUserAndDateGreaterThanEqual(user, goal.getStartDate());

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (Transaction t : transactions) {
            if (t.getType() == CategoryType.INCOME) {
                totalIncome = totalIncome.add(t.getAmount());
            } else if (t.getType() == CategoryType.EXPENSE) {
                totalExpenses = totalExpenses.add(t.getAmount());
            }
        }

        BigDecimal currentProgress = totalIncome.subtract(totalExpenses);

        Double progressPercentage = 0.0;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0 && currentProgress.compareTo(BigDecimal.ZERO) > 0) {
            progressPercentage = currentProgress
                    .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        BigDecimal remainingAmount = goal.getTargetAmount().subtract(currentProgress);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        return new GoalResponse(
                goal.getId(),
                goal.getGoalName(),
                goal.getTargetAmount(),
                goal.getTargetDate(),
                goal.getStartDate(),
                currentProgress,
                progressPercentage,
                remainingAmount
        );
    }
}
