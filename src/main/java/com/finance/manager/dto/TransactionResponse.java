package com.finance.manager.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.finance.manager.entity.CategoryType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class TransactionResponse {

    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER_FLOAT)
    private BigDecimal amount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String category;
    private String description;
    private CategoryType type;

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, BigDecimal amount, LocalDate date, String category, String description, CategoryType type) {
        this.id = id;
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : null;
        this.date = date;
        this.category = category;
        this.description = description;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : null;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }
}
