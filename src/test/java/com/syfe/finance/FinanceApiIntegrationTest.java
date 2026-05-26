package com.syfe.finance;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class FinanceApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authentication required"));
    }

    @Test
    void registerLoginAndRejectDuplicateUsername() throws Exception {
        String username = uniqueEmail("auth");
        register(username)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.userId", notNullValue()));

        register(username)
                .andExpect(status().isConflict());

        postJson("/api/auth/login", Map.of("username", username, "password", "wrong"), null)
                .andExpect(status().isUnauthorized());

        MockHttpSession session = login(username);
        postJson("/api/auth/logout", Map.of(), session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }

    @Test
    void supportsCategoriesTransactionsGoalsAndReports() throws Exception {
        MockHttpSession session = registerAndLogin("happy");

        mockMvc.perform(get("/api/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[*].name", hasItem("Salary")))
                .andExpect(jsonPath("$.categories[*].name", hasItem("Food")));

        postJson("/api/categories", Map.of("name", "Freelance", "type", "INCOME"), session)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Freelance"))
                .andExpect(jsonPath("$.isCustom").value(true));

        postJson("/api/categories", Map.of("name", "Freelance", "type", "INCOME"), session)
                .andExpect(status().isConflict());

        long salaryId = createTransaction(session, "5000.00", "2024-01-15", "Salary", "January salary")
                .path("id").asLong();
        createTransaction(session, "1200.00", "2024-01-20", "Food", "Groceries");
        createTransaction(session, "750.00", "2024-02-02", "Freelance", "Side project");

        mockMvc.perform(get("/api/transactions").param("type", "EXPENSE").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions.length()").value(1))
                .andExpect(jsonPath("$.transactions[0].category").value("Food"));

        mockMvc.perform(get("/api/reports/monthly/2024/1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome.Salary").value(5000.00))
                .andExpect(jsonPath("$.totalExpenses.Food").value(1200.00))
                .andExpect(jsonPath("$.netSavings").value(3800.00));

        postJson("/api/goals", goalBody("Emergency Fund", "5000.00", "2030-01-01", "2024-01-01"), session)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentProgress").value(4550.00))
                .andExpect(jsonPath("$.remainingAmount").value(450.00));

        postJson("/api/categories", Map.of("name", "Freelance", "type", "INCOME"), session)
                .andExpect(status().isConflict());

        postJson("/api/categories", Map.of("name", "Travel", "type", "EXPENSE"), session)
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/categories/Travel").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));

        putJson("/api/transactions/" + salaryId, Map.of("amount", "5500.00", "description", "Updated salary"), session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(5500.00))
                .andExpect(jsonPath("$.date").value("2024-01-15"));

        mockMvc.perform(get("/api/reports/yearly/2024").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome.Salary").value(5500.00))
                .andExpect(jsonPath("$.totalIncome.Freelance").value(750.00))
                .andExpect(jsonPath("$.netSavings").value(5050.00));
    }

    @Test
    void preventsCrossUserAccess() throws Exception {
        MockHttpSession owner = registerAndLogin("owner");
        MockHttpSession outsider = registerAndLogin("outsider");

        long transactionId = createTransaction(owner, "100.00", "2024-03-01", "Salary", "Private")
                .path("id").asLong();
        long goalId = read(postJson("/api/goals", goalBody("Private Goal", "1000.00", "2030-01-01", "2024-01-01"), owner)
                .andExpect(status().isCreated())
                .andReturn()).path("id").asLong();

        putJson("/api/transactions/" + transactionId, Map.of("amount", "200.00"), outsider)
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/goals/" + goalId).session(outsider))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsKnownInvalidRequests() throws Exception {
        MockHttpSession session = registerAndLogin("invalid");

        createTransactionResult(session, "10.00", "2099-01-01", "Salary", "Future")
                .andExpect(status().isBadRequest());

        createTransactionResult(session, "10.00", "2024-01-01", "Missing", "Unknown category")
                .andExpect(status().isBadRequest());

        long transactionId = createTransaction(session, "10.00", "2024-01-01", "Salary", "Good")
                .path("id").asLong();

        putJson("/api/transactions/" + transactionId, Map.of("date", "2024-01-02"), session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2024-01-01"));

        mockMvc.perform(delete("/api/categories/Salary").session(session))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/reports/monthly/2024/13").session(session))
                .andExpect(status().isBadRequest());
    }

    private MockHttpSession registerAndLogin(String prefix) throws Exception {
        String username = uniqueEmail(prefix);
        register(username).andExpect(status().isCreated());
        return login(username);
    }

    private ResultActions register(String username) throws Exception {
        return postJson("/api/auth/register", Map.of(
                "username", username,
                "password", "password123",
                "fullName", "Test User",
                "phoneNumber", "+1234567890"
        ), null);
    }

    private MockHttpSession login(String username) throws Exception {
        MvcResult result = postJson("/api/auth/login", Map.of(
                "username", username,
                "password", "password123"
        ), null).andExpect(status().isOk()).andReturn();
        HttpSession session = result.getRequest().getSession(false);
        if (!(session instanceof MockHttpSession mockHttpSession)) {
            throw new AssertionError("Expected login to create a MockHttpSession");
        }
        return mockHttpSession;
    }

    private JsonNode createTransaction(
            MockHttpSession session,
            String amount,
            String date,
            String category,
            String description
    ) throws Exception {
        return read(createTransactionResult(session, amount, date, category, description)
                .andExpect(status().isCreated())
                .andReturn());
    }

    private ResultActions createTransactionResult(
            MockHttpSession session,
            String amount,
            String date,
            String category,
            String description
    ) throws Exception {
        return postJson("/api/transactions", Map.of(
                "amount", amount,
                "date", date,
                "category", category,
                "description", description
        ), session);
    }

    private ResultActions postJson(String path, Object body, MockHttpSession session) throws Exception {
        var request = post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (session != null) {
            request.session(session);
        }
        return mockMvc.perform(request);
    }

    private ResultActions putJson(String path, Object body, MockHttpSession session) throws Exception {
        var request = put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
        if (session != null) {
            request.session(session);
        }
        return mockMvc.perform(request);
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private Map<String, String> goalBody(String goalName, String targetAmount, String targetDate, String startDate) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("goalName", goalName);
        body.put("targetAmount", targetAmount);
        body.put("targetDate", targetDate);
        body.put("startDate", startDate);
        return body;
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
