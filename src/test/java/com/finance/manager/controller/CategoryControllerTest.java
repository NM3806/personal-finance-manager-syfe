package com.finance.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.manager.dto.CreateCategoryRequest;
import com.finance.manager.dto.LoginRequest;
import com.finance.manager.dto.RegisterRequest;
import com.finance.manager.entity.CategoryType;
import com.finance.manager.repository.CategoryRepository;
import com.finance.manager.repository.UserRepository;
import com.finance.manager.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        categoryService.initDefaultCategories();
    }

    private MockHttpSession loginUser(String username, String password) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(username, password, "User", "+1234567890");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @Test
    void getCategories_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));
    }

    @Test
    void getCategories_authenticated_returnsDefaults() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        mockMvc.perform(get("/api/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.categories.length()").value(7))
                .andExpect(jsonPath("$.categories[0].name").value("Salary"))
                .andExpect(jsonPath("$.categories[0].type").value("INCOME"))
                .andExpect(jsonPath("$.categories[0].isCustom").value(false));
    }

    @Test
    void createCategory_success() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");
        CreateCategoryRequest request = new CreateCategoryRequest("SideBusinessIncome", CategoryType.INCOME);

        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("SideBusinessIncome"))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.isCustom").value(true));

        mockMvc.perform(get("/api/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(8));
    }

    @Test
    void createCategory_duplicateDefault_returns409Conflict() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");
        CreateCategoryRequest request = new CreateCategoryRequest("Salary", CategoryType.INCOME);

        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createCategory_duplicateCustom_returns409Conflict() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");
        CreateCategoryRequest request = new CreateCategoryRequest("CustomIncome", CategoryType.INCOME);

        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createCategory_invalidInput_returns400BadRequest() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategory_defaultCategory_returns400BadRequest() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        mockMvc.perform(delete("/api/categories/Salary").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deleteCategory_customCategory_returns200Ok() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");
        CreateCategoryRequest request = new CreateCategoryRequest("SideBusinessIncome", CategoryType.INCOME);

        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/categories/SideBusinessIncome").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
    }

    @Test
    void deleteCategory_notFound_returns404NotFound() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        mockMvc.perform(delete("/api/categories/NonExistent").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void category_userIsolation() throws Exception {
        MockHttpSession session1 = loginUser("user1@example.com", "password123");
        MockHttpSession session2 = loginUser("user2@example.com", "password123");

        CreateCategoryRequest request = new CreateCategoryRequest("User1Category", CategoryType.EXPENSE);
        mockMvc.perform(post("/api/categories")
                        .session(session1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // User 2 should NOT see User 1's custom category
        mockMvc.perform(get("/api/categories").session(session2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(7));

        // User 2 cannot delete User 1's custom category (returns 403 Forbidden)
        mockMvc.perform(delete("/api/categories/User1Category").session(session2))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").exists());
    }
}
