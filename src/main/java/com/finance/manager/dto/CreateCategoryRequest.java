package com.finance.manager.dto;

import com.finance.manager.entity.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    @NotNull(message = "Category type is required (INCOME or EXPENSE)")
    private CategoryType type;

    public CreateCategoryRequest() {
    }

    public CreateCategoryRequest(String name, CategoryType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }
}
