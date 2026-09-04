package com.finance.manager.service;

import com.finance.manager.dto.CategoryListResponse;
import com.finance.manager.dto.CategoryResponse;
import com.finance.manager.dto.CreateCategoryRequest;
import com.finance.manager.dto.MessageResponse;
import com.finance.manager.entity.Category;
import com.finance.manager.entity.CategoryType;
import com.finance.manager.entity.User;
import com.finance.manager.exception.BadRequestException;
import com.finance.manager.exception.ConflictException;
import com.finance.manager.exception.ForbiddenException;
import com.finance.manager.exception.ResourceNotFoundException;
import com.finance.manager.repository.CategoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final AuthService authService;

    public CategoryService(CategoryRepository categoryRepository, AuthService authService) {
        this.categoryRepository = categoryRepository;
        this.authService = authService;
    }

    @PostConstruct
    @Transactional
    public void initDefaultCategories() {
        seedDefaultCategory("Salary", CategoryType.INCOME);
        seedDefaultCategory("Food", CategoryType.EXPENSE);
        seedDefaultCategory("Rent", CategoryType.EXPENSE);
        seedDefaultCategory("Transportation", CategoryType.EXPENSE);
        seedDefaultCategory("Entertainment", CategoryType.EXPENSE);
        seedDefaultCategory("Healthcare", CategoryType.EXPENSE);
        seedDefaultCategory("Utilities", CategoryType.EXPENSE);
    }

    private void seedDefaultCategory(String name, CategoryType type) {
        if (!categoryRepository.existsByNameIgnoreCaseAndUserIsNull(name)) {
            categoryRepository.save(new Category(name, type, null, false));
        }
    }

    public CategoryListResponse getAllCategories() {
        User user = authService.getCurrentUser();
        List<Category> defaultCategories = categoryRepository.findByUserIsNullOrderByIdAsc();
        List<Category> userCategories = categoryRepository.findByUserOrderByIdAsc(user);

        List<CategoryResponse> responses = new ArrayList<>();
        for (Category cat : defaultCategories) {
            responses.add(new CategoryResponse(cat.getName(), cat.getType(), false));
        }
        for (Category cat : userCategories) {
            responses.add(new CategoryResponse(cat.getName(), cat.getType(), true));
        }

        return new CategoryListResponse(responses);
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        User user = authService.getCurrentUser();
        String trimmedName = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCaseAndUserIsNull(trimmedName) ||
                categoryRepository.existsByNameIgnoreCaseAndUser(trimmedName, user)) {
            throw new ConflictException("Category already exists: " + trimmedName);
        }

        Category category = new Category(trimmedName, request.getType(), user, true);
        Category saved = categoryRepository.save(category);
        return new CategoryResponse(saved.getName(), saved.getType(), true);
    }

    @Transactional
    public MessageResponse deleteCategory(String name) {
        User user = authService.getCurrentUser();
        String trimmedName = name.trim();

        if (categoryRepository.existsByNameIgnoreCaseAndUserIsNull(trimmedName)) {
            throw new BadRequestException("Cannot delete default category: " + trimmedName);
        }

        Optional<Category> userCategory = categoryRepository.findByNameIgnoreCaseAndUser(trimmedName, user);
        if (userCategory.isPresent()) {
            categoryRepository.delete(userCategory.get());
            return new MessageResponse("Category deleted successfully");
        }

        List<Category> allCategories = categoryRepository.findAll();
        boolean existsForOtherUser = allCategories.stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(trimmedName) && c.getUser() != null && !c.getUser().getId().equals(user.getId()));

        if (existsForOtherUser) {
            throw new ForbiddenException("Cannot delete another user's category: " + trimmedName);
        }

        throw new ResourceNotFoundException("Category not found: " + trimmedName);
    }

    public Category getValidCategory(String name, User user) {
        String trimmedName = name.trim();
        Optional<Category> defaultCat = categoryRepository.findByNameIgnoreCaseAndUserIsNull(trimmedName);
        if (defaultCat.isPresent()) {
            return defaultCat.get();
        }
        return categoryRepository.findByNameIgnoreCaseAndUser(trimmedName, user)
                .orElseThrow(() -> new BadRequestException("Invalid category: " + trimmedName));
    }
}
