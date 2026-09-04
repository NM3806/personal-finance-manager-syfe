package com.finance.manager.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.finance.manager.entity.CategoryType;

public class CategoryResponse {

    private String name;
    private CategoryType type;

    @JsonProperty("isCustom")
    private boolean isCustom;

    public CategoryResponse() {
    }

    public CategoryResponse(String name, CategoryType type, boolean isCustom) {
        this.name = name;
        this.type = type;
        this.isCustom = isCustom;
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

    @JsonProperty("isCustom")
    public boolean isCustom() {
        return isCustom;
    }

    @JsonProperty("isCustom")
    public void setCustom(boolean custom) {
        isCustom = custom;
    }
}
