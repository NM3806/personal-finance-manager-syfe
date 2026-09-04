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
import com.finance.manager.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuthService authService;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("user@example.com", "hash", "User One", "+1234567890");
        testUser.setId(1L);
    }

    @Test
    void initDefaultCategories_seedsMissing() {
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Salary")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Food")).thenReturn(true);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Rent")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Transportation")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Entertainment")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Healthcare")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Utilities")).thenReturn(false);

        categoryService.initDefaultCategories();

        verify(categoryRepository, org.mockito.Mockito.times(6)).save(any(Category.class));
    }

    @Test
    void getAllCategories_returnsDefaultsAndCustom() {
        when(authService.getCurrentUser()).thenReturn(testUser);

        Category defaultCat = new Category("Salary", CategoryType.INCOME, null, false);
        Category customCat = new Category("Freelance", CategoryType.INCOME, testUser, true);

        when(categoryRepository.findByUserIsNullOrderByIdAsc()).thenReturn(List.of(defaultCat));
        when(categoryRepository.findByUserOrderByIdAsc(testUser)).thenReturn(List.of(customCat));

        CategoryListResponse response = categoryService.getAllCategories();

        assertNotNull(response);
        assertEquals(2, response.getCategories().size());
        assertFalse(response.getCategories().get(0).isCustom());
        assertTrue(response.getCategories().get(1).isCustom());
    }

    @Test
    void createCategory_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        CreateCategoryRequest request = new CreateCategoryRequest("Freelance", CategoryType.INCOME);

        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Freelance")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUser("Freelance", testUser)).thenReturn(false);

        Category saved = new Category("Freelance", CategoryType.INCOME, testUser, true);
        saved.setId(10L);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("Freelance", response.getName());
        assertEquals(CategoryType.INCOME, response.getType());
        assertTrue(response.isCustom());
    }

    @Test
    void createCategory_duplicateDefault_throwsConflict() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        CreateCategoryRequest request = new CreateCategoryRequest("Salary", CategoryType.INCOME);

        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Salary")).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategory_duplicateUserCategory_throwsConflict() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        CreateCategoryRequest request = new CreateCategoryRequest("Freelance", CategoryType.INCOME);

        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Freelance")).thenReturn(false);
        when(categoryRepository.existsByNameIgnoreCaseAndUser("Freelance", testUser)).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_defaultCategory_throwsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Salary")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> categoryService.deleteCategory("Salary"));
    }

    @Test
    void deleteCategory_referencedByTransactions_throwsBadRequest() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Freelance")).thenReturn(false);

        Category customCat = new Category("Freelance", CategoryType.INCOME, testUser, true);
        when(categoryRepository.findByNameIgnoreCaseAndUser("Freelance", testUser)).thenReturn(Optional.of(customCat));
        when(transactionRepository.existsByCategory(customCat)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> categoryService.deleteCategory("Freelance"));
    }

    @Test
    void deleteCategory_customCategory_success() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Freelance")).thenReturn(false);

        Category customCat = new Category("Freelance", CategoryType.INCOME, testUser, true);
        when(categoryRepository.findByNameIgnoreCaseAndUser("Freelance", testUser)).thenReturn(Optional.of(customCat));

        MessageResponse response = categoryService.deleteCategory("Freelance");

        assertNotNull(response);
        assertEquals("Category deleted successfully", response.getMessage());
        verify(categoryRepository).delete(customCat);
    }

    @Test
    void deleteCategory_notFound_throwsNotFound() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("Unknown")).thenReturn(false);
        when(categoryRepository.findByNameIgnoreCaseAndUser("Unknown", testUser)).thenReturn(Optional.empty());
        when(categoryRepository.findAll()).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class, () -> categoryService.deleteCategory("Unknown"));
    }

    @Test
    void deleteCategory_otherUserCategory_throwsForbidden() {
        when(authService.getCurrentUser()).thenReturn(testUser);
        when(categoryRepository.existsByNameIgnoreCaseAndUserIsNull("OtherCustom")).thenReturn(false);
        when(categoryRepository.findByNameIgnoreCaseAndUser("OtherCustom", testUser)).thenReturn(Optional.empty());

        User otherUser = new User("other@example.com", "hash", "Other", "+111");
        otherUser.setId(2L);
        Category otherCat = new Category("OtherCustom", CategoryType.EXPENSE, otherUser, true);
        when(categoryRepository.findAll()).thenReturn(List.of(otherCat));

        assertThrows(ForbiddenException.class, () -> categoryService.deleteCategory("OtherCustom"));
    }

    @Test
    void getValidCategory_returnsCategory() {
        Category defaultCat = new Category("Salary", CategoryType.INCOME, null, false);
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Salary")).thenReturn(Optional.of(defaultCat));

        Category result = categoryService.getValidCategory("Salary", testUser);
        assertEquals("Salary", result.getName());
    }

    @Test
    void getValidCategory_invalid_throwsBadRequest() {
        when(categoryRepository.findByNameIgnoreCaseAndUserIsNull("Invalid")).thenReturn(Optional.empty());
        when(categoryRepository.findByNameIgnoreCaseAndUser("Invalid", testUser)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> categoryService.getValidCategory("Invalid", testUser));
    }
}
