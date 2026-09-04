package com.finance.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.manager.dto.CreateGoalRequest;
import com.finance.manager.dto.CreateTransactionRequest;
import com.finance.manager.dto.LoginRequest;
import com.finance.manager.dto.RegisterRequest;
import com.finance.manager.dto.UpdateGoalRequest;
import com.finance.manager.repository.CategoryRepository;
import com.finance.manager.repository.SavingsGoalRepository;
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
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        savingsGoalRepository.deleteAll();
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
        mockMvc.perform(get("/api/goals"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createGoal_and_calculateProgress() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        // Transaction before goal startDate (2024-12-01) - should not contribute
        CreateTransactionRequest oldTx = new CreateTransactionRequest(BigDecimal.valueOf(10000.00), LocalDate.of(2024, 12, 1), "Salary", "Old Salary");
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(oldTx)))
                .andExpect(status().isCreated());

        LocalDate targetDate = LocalDate.now().plusYears(1);
        LocalDate startDate = LocalDate.now().minusMonths(1);

        CreateTransactionRequest incomeTx = new CreateTransactionRequest(BigDecimal.valueOf(3000.00), LocalDate.now().minusDays(5), "Salary", "Jan Salary");
        CreateTransactionRequest expenseTx = new CreateTransactionRequest(BigDecimal.valueOf(2000.00), LocalDate.now().minusDays(2), "Rent", "Jan Rent");

        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(incomeTx)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(expenseTx)))
                .andExpect(status().isCreated());

        CreateGoalRequest goalReq = new CreateGoalRequest(
                "Emergency Fund",
                BigDecimal.valueOf(5000.00),
                targetDate,
                startDate
        );

        // Progress = 3000 - 2000 = 1000. Progress% = (1000 / 5000) * 100 = 20.0. Remaining = 4000.
        mockMvc.perform(post("/api/goals")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.goalName").value("Emergency Fund"))
                .andExpect(jsonPath("$.targetAmount").value(5000.00))
                .andExpect(jsonPath("$.targetDate").value(targetDate.toString()))
                .andExpect(jsonPath("$.startDate").value(startDate.toString()))
                .andExpect(jsonPath("$.currentProgress").value(1000.00))
                .andExpect(jsonPath("$.progressPercentage").value(20.0))
                .andExpect(jsonPath("$.remainingAmount").value(4000.00));
    }

    @Test
    void createGoal_pastTargetDate_returns400() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");
        CreateGoalRequest goalReq = new CreateGoalRequest(
                "Emergency Fund",
                BigDecimal.valueOf(5000.00),
                LocalDate.now().minusDays(1),
                null
        );

        mockMvc.perform(post("/api/goals")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getGoalById_and_updateGoal() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        CreateTransactionRequest incomeTx = new CreateTransactionRequest(BigDecimal.valueOf(1000.00), LocalDate.now().minusDays(5), "Salary", "Jan Salary");
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(incomeTx)))
                .andExpect(status().isCreated());

        LocalDate targetDate = LocalDate.now().plusMonths(6);
        LocalDate startDate = LocalDate.now().minusMonths(1);

        CreateGoalRequest goalReq = new CreateGoalRequest(
                "Emergency Fund",
                BigDecimal.valueOf(5000.00),
                targetDate,
                startDate
        );

        MvcResult createResult = mockMvc.perform(post("/api/goals")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // Get by ID
        mockMvc.perform(get("/api/goals/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.currentProgress").value(1000.00))
                .andExpect(jsonPath("$.remainingAmount").value(4000.00));

        // Update target amount to 6000 and target date to 12 months in future
        LocalDate updatedTargetDate = LocalDate.now().plusMonths(12);
        UpdateGoalRequest updateReq = new UpdateGoalRequest(BigDecimal.valueOf(6000.00), updatedTargetDate);
        mockMvc.perform(put("/api/goals/" + id)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetAmount").value(6000.00))
                .andExpect(jsonPath("$.targetDate").value(updatedTargetDate.toString()))
                .andExpect(jsonPath("$.currentProgress").value(1000.00))
                .andExpect(jsonPath("$.progressPercentage").value(16.67))
                .andExpect(jsonPath("$.remainingAmount").value(5000.00));
    }

    @Test
    void deleteGoal_success() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        CreateGoalRequest goalReq = new CreateGoalRequest("Trip", BigDecimal.valueOf(2000.00), LocalDate.now().plusMonths(6), null);
        MvcResult createResult = mockMvc.perform(post("/api/goals")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(goalReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(delete("/api/goals/" + id).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Goal deleted successfully"));

        mockMvc.perform(get("/api/goals").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals", hasSize(0)));
    }

    @Test
    void deletedTransactions_doNotReflectInGoals() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        CreateTransactionRequest incomeTx = new CreateTransactionRequest(BigDecimal.valueOf(2000.00), LocalDate.now().minusDays(5), "Salary", "Jan Salary");
        MvcResult txResult = mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(incomeTx)))
                .andExpect(status().isCreated())
                .andReturn();

        Long txId = objectMapper.readTree(txResult.getResponse().getContentAsString()).get("id").asLong();

        LocalDate targetDate = LocalDate.now().plusYears(1);
        LocalDate startDate = LocalDate.now().minusMonths(1);

        CreateGoalRequest goalReq = new CreateGoalRequest("Emergency Fund", BigDecimal.valueOf(5000.00), targetDate, startDate);
        MvcResult goalResult = mockMvc.perform(post("/api/goals").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(goalReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long goalId = objectMapper.readTree(goalResult.getResponse().getContentAsString()).get("id").asLong();

        // Progress initially 2000
        mockMvc.perform(get("/api/goals/" + goalId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProgress").value(2000.00));

        // Delete transaction
        mockMvc.perform(delete("/api/transactions/" + txId).session(session))
                .andExpect(status().isOk());

        // Progress now 0
        mockMvc.perform(get("/api/goals/" + goalId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentProgress").value(0.00))
                .andExpect(jsonPath("$.remainingAmount").value(5000.00));
    }

    @Test
    void goals_userIsolation() throws Exception {
        MockHttpSession session1 = loginUser("user1@example.com", "password123");
        MockHttpSession session2 = loginUser("user2@example.com", "password123");

        CreateGoalRequest goalReq = new CreateGoalRequest("User 1 Goal", BigDecimal.valueOf(5000.00), LocalDate.now().plusMonths(6), null);
        MvcResult createResult = mockMvc.perform(post("/api/goals").session(session1).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(goalReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long goalId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        // User 2 sees 0 goals
        mockMvc.perform(get("/api/goals").session(session2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goals", hasSize(0)));

        // User 2 cannot get User 1's goal (403 Forbidden)
        mockMvc.perform(get("/api/goals/" + goalId).session(session2))
                .andExpect(status().isForbidden());

        // User 2 cannot update User 1's goal (403 Forbidden)
        UpdateGoalRequest updateReq = new UpdateGoalRequest(BigDecimal.valueOf(9999.00), LocalDate.now().plusMonths(12));
        mockMvc.perform(put("/api/goals/" + goalId).session(session2).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // User 2 cannot delete User 1's goal (403 Forbidden)
        mockMvc.perform(delete("/api/goals/" + goalId).session(session2))
                .andExpect(status().isForbidden());
    }
}
