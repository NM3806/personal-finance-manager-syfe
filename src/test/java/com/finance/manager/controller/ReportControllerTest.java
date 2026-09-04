package com.finance.manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.manager.dto.CreateCategoryRequest;
import com.finance.manager.dto.CreateTransactionRequest;
import com.finance.manager.dto.LoginRequest;
import com.finance.manager.dto.RegisterRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest {

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
        mockMvc.perform(get("/api/reports/monthly/2024/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reports/yearly/2024"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMonthlyReport_matchesPdfExample() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        // Custom category "Freelance"
        CreateCategoryRequest freelanceCat = new CreateCategoryRequest("Freelance", CategoryType.INCOME);
        mockMvc.perform(post("/api/categories").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(freelanceCat)))
                .andExpect(status().isCreated());

        // Transactions in Jan 2024
        CreateTransactionRequest t1 = new CreateTransactionRequest(BigDecimal.valueOf(3000.00), LocalDate.of(2024, 1, 5), "Salary", "Salary");
        CreateTransactionRequest t2 = new CreateTransactionRequest(BigDecimal.valueOf(500.00), LocalDate.of(2024, 1, 10), "Freelance", "Freelance");
        CreateTransactionRequest t3 = new CreateTransactionRequest(BigDecimal.valueOf(400.00), LocalDate.of(2024, 1, 15), "Food", "Food");
        CreateTransactionRequest t4 = new CreateTransactionRequest(BigDecimal.valueOf(1200.00), LocalDate.of(2024, 1, 20), "Rent", "Rent");
        CreateTransactionRequest t5 = new CreateTransactionRequest(BigDecimal.valueOf(200.00), LocalDate.of(2024, 1, 25), "Transportation", "Transportation");

        // Transaction in Feb 2024 (should not be in Jan report)
        CreateTransactionRequest t6 = new CreateTransactionRequest(BigDecimal.valueOf(1000.00), LocalDate.of(2024, 2, 1), "Salary", "Feb Salary");

        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t1)));
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t2)));
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t3)));
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t4)));
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t5)));
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t6)));

        mockMvc.perform(get("/api/reports/monthly/2024/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value(1))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.totalIncome.Salary").value(3000.00))
                .andExpect(jsonPath("$.totalIncome.Freelance").value(500.00))
                .andExpect(jsonPath("$.totalExpenses.Food").value(400.00))
                .andExpect(jsonPath("$.totalExpenses.Rent").value(1200.00))
                .andExpect(jsonPath("$.totalExpenses.Transportation").value(200.00))
                .andExpect(jsonPath("$.netSavings").value(1700.00));
    }

    @Test
    void getYearlyReport_matchesPdfExample() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        CreateCategoryRequest freelanceCat = new CreateCategoryRequest("Freelance", CategoryType.INCOME);
        mockMvc.perform(post("/api/categories").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(freelanceCat)))
                .andExpect(status().isCreated());

        CreateTransactionRequest t1 = new CreateTransactionRequest(BigDecimal.valueOf(36000.00), LocalDate.of(2024, 3, 1), "Salary", "Annual Salary");
        CreateTransactionRequest t2 = new CreateTransactionRequest(BigDecimal.valueOf(6000.00), LocalDate.of(2024, 6, 1), "Freelance", "Annual Freelance");
        CreateTransactionRequest t3 = new CreateTransactionRequest(BigDecimal.valueOf(4800.00), LocalDate.of(2024, 7, 1), "Food", "Annual Food");
        CreateTransactionRequest t4 = new CreateTransactionRequest(BigDecimal.valueOf(14400.00), LocalDate.of(2024, 8, 1), "Rent", "Annual Rent");
        CreateTransactionRequest t5 = new CreateTransactionRequest(BigDecimal.valueOf(2400.00), LocalDate.of(2024, 9, 1), "Transportation", "Annual Transportation");

        // Transaction in 2023 (should not be in 2024 report)
        CreateTransactionRequest tOld = new CreateTransactionRequest(BigDecimal.valueOf(5000.00), LocalDate.of(2023, 12, 31), "Salary", "2023 Salary");

        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t1)));
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t2)));
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t3)));
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t4)));
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t5)));
        mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(tOld)));

        mockMvc.perform(get("/api/reports/yearly/2024").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.totalIncome.Salary").value(36000.00))
                .andExpect(jsonPath("$.totalIncome.Freelance").value(6000.00))
                .andExpect(jsonPath("$.totalExpenses.Food").value(4800.00))
                .andExpect(jsonPath("$.totalExpenses.Rent").value(14400.00))
                .andExpect(jsonPath("$.totalExpenses.Transportation").value(2400.00))
                .andExpect(jsonPath("$.netSavings").value(20400.00));
    }

    @Test
    void invalidMonth_returns400() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        mockMvc.perform(get("/api/reports/monthly/2024/13").session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deletedTransactions_excludedFromReports() throws Exception {
        MockHttpSession session = loginUser("user1@example.com", "password123");

        CreateTransactionRequest t1 = new CreateTransactionRequest(BigDecimal.valueOf(3000.00), LocalDate.of(2024, 1, 5), "Salary", "Salary");
        MvcResult txResult = mockMvc.perform(post("/api/transactions").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t1)))
                .andExpect(status().isCreated())
                .andReturn();

        Long txId = objectMapper.readTree(txResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/reports/monthly/2024/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netSavings").value(3000.00));

        // Delete transaction
        mockMvc.perform(delete("/api/transactions/" + txId).session(session))
                .andExpect(status().isOk());

        // Report now reflects 0
        mockMvc.perform(get("/api/reports/monthly/2024/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netSavings").value(0.00));
    }

    @Test
    void reports_userIsolation() throws Exception {
        MockHttpSession session1 = loginUser("user1@example.com", "password123");
        MockHttpSession session2 = loginUser("user2@example.com", "password123");

        CreateTransactionRequest t1 = new CreateTransactionRequest(BigDecimal.valueOf(5000.00), LocalDate.of(2024, 1, 5), "Salary", "User 1 Salary");
        mockMvc.perform(post("/api/transactions").session(session1).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(t1)))
                .andExpect(status().isCreated());

        // User 2's monthly report is empty
        mockMvc.perform(get("/api/reports/monthly/2024/1").session(session2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netSavings").value(0.00))
                .andExpect(jsonPath("$.totalIncome").isEmpty());
    }
}
