package com.finance.manager.entity;

import com.finance.manager.dto.CategoryListResponse;
import com.finance.manager.dto.CategoryResponse;
import com.finance.manager.dto.CreateCategoryRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryEntityTest {

    @Test
    void testCategoryEntityGettersAndSetters() {
        Category category = new Category();
        category.setId(10L);
        category.setName("Bonus");
        category.setType(CategoryType.INCOME);
        User user = new User();
        user.setId(5L);
        category.setUser(user);
        category.setCustom(true);

        assertEquals(10L, category.getId());
        assertEquals("Bonus", category.getName());
        assertEquals(CategoryType.INCOME, category.getType());
        assertEquals(user, category.getUser());
        assertTrue(category.isCustom());
    }

    @Test
    void testCategoryDtos() {
        CategoryResponse response = new CategoryResponse("Food", CategoryType.EXPENSE, false);
        assertEquals("Food", response.getName());
        assertEquals(CategoryType.EXPENSE, response.getType());
        assertFalse(response.isCustom());

        response.setName("Salary");
        response.setType(CategoryType.INCOME);
        response.setCustom(true);
        assertEquals("Salary", response.getName());
        assertEquals(CategoryType.INCOME, response.getType());
        assertTrue(response.isCustom());

        CategoryListResponse listResponse = new CategoryListResponse(List.of(response));
        assertEquals(1, listResponse.getCategories().size());
        listResponse.setCategories(List.of());
        assertEquals(0, listResponse.getCategories().size());

        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Travel");
        request.setType(CategoryType.EXPENSE);
        assertEquals("Travel", request.getName());
        assertEquals(CategoryType.EXPENSE, request.getType());
    }
}
