package com.finance.manager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public class UpdateTransactionRequest {

    @Positive(message = "Amount must be a positive decimal value")
    private BigDecimal amount;

    private String category;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    public UpdateTransactionRequest() {
    }

    public UpdateTransactionRequest(BigDecimal amount, String description) {
        this.amount = amount;
        this.description = description;
    }

    public UpdateTransactionRequest(BigDecimal amount, String category, String description) {
        this.amount = amount;
        this.category = category;
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
