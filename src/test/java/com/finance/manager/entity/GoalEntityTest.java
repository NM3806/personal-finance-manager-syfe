package com.finance.manager.entity;

import com.finance.manager.dto.CreateGoalRequest;
import com.finance.manager.dto.GoalListResponse;
import com.finance.manager.dto.GoalResponse;
import com.finance.manager.dto.UpdateGoalRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GoalEntityTest {

    @Test
    void testSavingsGoalEntityGettersAndSetters() {
        SavingsGoal goal = new SavingsGoal();
        goal.setId(10L);
        goal.setGoalName("Car");
        goal.setTargetAmount(BigDecimal.valueOf(15000));
        LocalDate targetDate = LocalDate.of(2026, 6, 1);
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        goal.setTargetDate(targetDate);
        goal.setStartDate(startDate);

        User user = new User();
        user.setId(1L);
        goal.setUser(user);
        LocalDateTime now = LocalDateTime.now();
        goal.setCreatedAt(now);

        assertEquals(10L, goal.getId());
        assertEquals("Car", goal.getGoalName());
        assertEquals(BigDecimal.valueOf(15000), goal.getTargetAmount());
        assertEquals(targetDate, goal.getTargetDate());
        assertEquals(startDate, goal.getStartDate());
        assertEquals(user, goal.getUser());
        assertEquals(now, goal.getCreatedAt());
    }

    @Test
    void testSavingsGoalDtos() {
        LocalDate targetDate = LocalDate.of(2026, 6, 1);
        LocalDate startDate = LocalDate.of(2025, 1, 1);

        CreateGoalRequest createReq = new CreateGoalRequest();
        createReq.setGoalName("House");
        createReq.setTargetAmount(BigDecimal.valueOf(50000));
        createReq.setTargetDate(targetDate);
        createReq.setStartDate(startDate);

        assertEquals("House", createReq.getGoalName());
        assertEquals(BigDecimal.valueOf(50000), createReq.getTargetAmount());
        assertEquals(targetDate, createReq.getTargetDate());
        assertEquals(startDate, createReq.getStartDate());

        UpdateGoalRequest updateReq = new UpdateGoalRequest();
        updateReq.setGoalName("House Renovation");
        updateReq.setTargetAmount(BigDecimal.valueOf(60000));
        updateReq.setTargetDate(targetDate.plusMonths(6));

        assertEquals("House Renovation", updateReq.getGoalName());
        assertEquals(BigDecimal.valueOf(60000), updateReq.getTargetAmount());
        assertEquals(targetDate.plusMonths(6), updateReq.getTargetDate());

        GoalResponse response = new GoalResponse();
        response.setId(1L);
        response.setGoalName("Emergency");
        response.setTargetAmount(BigDecimal.valueOf(5000));
        response.setTargetDate(targetDate);
        response.setStartDate(startDate);
        response.setCurrentProgress(BigDecimal.valueOf(1000));
        response.setProgressPercentage(20.0);
        response.setRemainingAmount(BigDecimal.valueOf(4000));

        assertEquals(1L, response.getId());
        assertEquals("Emergency", response.getGoalName());
        assertEquals(BigDecimal.valueOf(5000).setScale(2), response.getTargetAmount());
        assertEquals(targetDate, response.getTargetDate());
        assertEquals(startDate, response.getStartDate());
        assertEquals(BigDecimal.valueOf(1000).setScale(2), response.getCurrentProgress());
        assertEquals(20.0, response.getProgressPercentage());
        assertEquals(BigDecimal.valueOf(4000).setScale(2), response.getRemainingAmount());

        GoalListResponse listResponse = new GoalListResponse();
        listResponse.setGoals(List.of(response));
        assertEquals(1, listResponse.getGoals().size());
    }
}
