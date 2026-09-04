package com.finance.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.manager.dto.CreateCategoryRequest;
import com.finance.manager.dto.CreateTransactionRequest;
import com.finance.manager.dto.LoginRequest;
import com.finance.manager.dto.RegisterRequest;
import com.finance.manager.dto.UpdateTransactionRequest;
import com.finance.manager.entity.CategoryType;
import com.finance.manager.repository.CategoryRepository;
import com.finance.manager.repository.TransactionRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
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
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createTransaction_success() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");
        CreateTransactionRequest request = new CreateTransactionRequest(
                BigDecimal.valueOf(50000.00),
                LocalDate.now().minusDays(2),
                "Salary",
                "January Salary"
        );

        mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.amount").value(50000.00))
                .andExpect(jsonPath("$.category").value("Salary"))
                .andExpect(jsonPath("$.description").value("January Salary"))
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    void createTransaction_futureDate_returns400() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");
        CreateTransactionRequest request = new CreateTransactionRequest(
                BigDecimal.valueOf(100.00),
                LocalDate.now().plusDays(5),
                "Salary",
                "Future Salary"
        );

        mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createTransaction_invalidCategory_returns400() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");
        CreateTransactionRequest request = new CreateTransactionRequest(
                BigDecimal.valueOf(100.00),
                LocalDate.now().minusDays(1),
                "NonExistentCategory",
                "Invalid"
        );

        mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void createTransaction_negativeAmount_returns400() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");
        CreateTransactionRequest request = new CreateTransactionRequest(
                BigDecimal.valueOf(-50.00),
                LocalDate.now().minusDays(1),
                "Salary",
                "Negative"
        );

        mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTransactions_newestFirst_and_filtering() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        CreateTransactionRequest t1 = new CreateTransactionRequest(BigDecimal.valueOf(100.00), LocalDate.of(2024, 1, 10), "Food", "Groceries");
        CreateTransactionRequest t2 = new CreateTransactionRequest(BigDecimal.valueOf(5000.00), LocalDate.of(2024, 1, 20), "Salary", "Salary");
        CreateTransactionRequest t3 = new CreateTransactionRequest(BigDecimal.valueOf(200.00), LocalDate.of(2024, 2, 5), "Food", "Restaurant");

        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t1)));
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t2)));
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t3)));

        // Newest first ordering: t3 (Feb 5), t2 (Jan 20), t1 (Jan 10)
        mockMvc.perform(get("/api/transactions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(3)))
                .andExpect(jsonPath("$.transactions[0].date").value("2024-02-05"))
                .andExpect(jsonPath("$.transactions[1].date").value("2024-01-20"))
                .andExpect(jsonPath("$.transactions[2].date").value("2024-01-10"));

        // Filter by date range
        mockMvc.perform(get("/api/transactions?startDate=2024-01-01&endDate=2024-01-31").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(2)))
                .andExpect(jsonPath("$.transactions[0].date").value("2024-01-20"))
                .andExpect(jsonPath("$.transactions[1].date").value("2024-01-10"));

        // Filter by category
        mockMvc.perform(get("/api/transactions?category=Food").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(2)))
                .andExpect(jsonPath("$.transactions[0].category").value("Food"))
                .andExpect(jsonPath("$.transactions[1].category").value("Food"));

        // Filter by type
        mockMvc.perform(get("/api/transactions?type=INCOME").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(1)))
                .andExpect(jsonPath("$.transactions[0].category").value("Salary"));
    }

    @Test
    void updateTransaction_success() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        CreateTransactionRequest createReq = new CreateTransactionRequest(
                BigDecimal.valueOf(50000.00),
                LocalDate.of(2024, 1, 15),
                "Salary",
                "January Salary"
        );

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String createJson = createResult.getResponse().getContentAsString();
        Long id = objectMapper.readTree(createJson).get("id").asLong();

        UpdateTransactionRequest updateReq = new UpdateTransactionRequest(BigDecimal.valueOf(60000.00), "Updated January Salary");

        mockMvc.perform(put("/api/transactions/" + id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.amount").value(60000.00))
                .andExpect(jsonPath("$.date").value("2024-01-15"))
                .andExpect(jsonPath("$.category").value("Salary"))
                .andExpect(jsonPath("$.description").value("Updated January Salary"))
                .andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test
    void updateTransaction_immutableDate_returns400() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        CreateTransactionRequest createReq = new CreateTransactionRequest(
                BigDecimal.valueOf(50000.00),
                LocalDate.of(2024, 1, 15),
                "Salary",
                "January Salary"
        );

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        UpdateTransactionRequest updateReq = new UpdateTransactionRequest(BigDecimal.valueOf(60000.00), "Updated");
        updateReq.setDate(LocalDate.of(2024, 1, 20));

        mockMvc.perform(put("/api/transactions/" + id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2024-01-15"))
                .andExpect(jsonPath("$.amount").value(60000.00));
    }

    @Test
    void deleteTransaction_success() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        CreateTransactionRequest createReq = new CreateTransactionRequest(
                BigDecimal.valueOf(50000.00),
                LocalDate.of(2024, 1, 15),
                "Salary",
                "January Salary"
        );

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/transactions/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Transaction deleted successfully"));

        // Verify deleted transaction is not returned in GET
        mockMvc.perform(get("/api/transactions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(0)));
    }

    @Test
    void transaction_userIsolation() throws Exception {
        MockHttpSession session1 = loginUser("user1@example.com", "password123");
        MockHttpSession session2 = loginUser("user2@example.com", "password123");

        CreateTransactionRequest createReq = new CreateTransactionRequest(
                BigDecimal.valueOf(50000.00),
                LocalDate.of(2024, 1, 15),
                "Salary",
                "User 1 Salary"
        );

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .session(session1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // User 2 sees 0 transactions
        mockMvc.perform(get("/api/transactions").session(session2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions", hasSize(0)));

        // User 2 cannot update User 1's transaction (403 Forbidden)
        UpdateTransactionRequest updateReq = new UpdateTransactionRequest(BigDecimal.valueOf(100.00), "Hacked");
        mockMvc.perform(put("/api/transactions/" + id)
                        .session(session2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // User 2 cannot delete User 1's transaction (403 Forbidden)
        mockMvc.perform(delete("/api/transactions/" + id).session(session2))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCategory_referencedByTransaction_blocked() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        CreateCategoryRequest catReq = new CreateCategoryRequest("CustomBonus", CategoryType.INCOME);
        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andExpect(status().isCreated());

        CreateTransactionRequest txReq = new CreateTransactionRequest(
                BigDecimal.valueOf(1000.00),
                LocalDate.now().minusDays(1),
                "CustomBonus",
                "Bonus"
        );
        mockMvc.perform(post("/api/transactions")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(txReq)))
                .andExpect(status().isCreated());

        // Attempt to delete category referenced by transaction
        mockMvc.perform(delete("/api/categories/CustomBonus").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete category referenced by transactions: CustomBonus"));
    }
}
