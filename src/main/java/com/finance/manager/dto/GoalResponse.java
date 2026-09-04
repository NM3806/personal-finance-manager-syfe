package com.finance.manager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class GoalResponse {

    private Long id;
    private String goalName;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
    private BigDecimal targetAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
    private BigDecimal currentProgress;

    private Double progressPercentage;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
    private BigDecimal remainingAmount;

    public GoalResponse() {
    }

    public GoalResponse(Long id, String goalName, BigDecimal targetAmount, LocalDate targetDate, LocalDate startDate,
                        BigDecimal currentProgress, Double progressPercentage, BigDecimal remainingAmount) {
        this.id = id;
        this.goalName = goalName;
        this.targetAmount = targetAmount != null ? targetAmount.setScale(2, RoundingMode.HALF_UP) : null;
        this.targetDate = targetDate;
        this.startDate = startDate;
        this.currentProgress = currentProgress != null ? (currentProgress.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : currentProgress.setScale(2, RoundingMode.HALF_UP)) : null;
        this.progressPercentage = progressPercentage;
        this.remainingAmount = remainingAmount != null ? remainingAmount.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGoalName() {
        return goalName;
    }

    public void setGoalName(String goalName) {
        this.goalName = goalName;
    }

    public BigDecimal getTargetAmount() {
        return targetAmount;
    }

    public void setTargetAmount(BigDecimal targetAmount) {
        this.targetAmount = targetAmount != null ? targetAmount.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public BigDecimal getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(BigDecimal currentProgress) {
        this.currentProgress = currentProgress != null ? (currentProgress.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : currentProgress.setScale(2, RoundingMode.HALF_UP)) : null;
    }

    public Double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(Double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount != null ? remainingAmount.setScale(2, RoundingMode.HALF_UP) : null;
    }
}
