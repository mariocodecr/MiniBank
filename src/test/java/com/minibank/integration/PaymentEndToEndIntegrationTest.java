package com.minibank.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.accounts.adapter.web.dto.CreateAccountRequest;
import com.minibank.accounts.domain.Currency;
import com.minibank.payments.adapter.web.dto.CreatePaymentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Testcontainers
@Transactional
@AutoConfigureMockMvc
@Disabled("TestContainers integration tests disabled - requires Docker environment")
class PaymentEndToEndIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("minibank_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void shouldCompleteEndToEndPaymentFlow() throws Exception {
        // Step 1: Create source account with balance
        UUID sourceUserId = UUID.randomUUID();
        CreateAccountRequest sourceAccountRequest = new CreateAccountRequest(sourceUserId, Currency.USD);
        
        String sourceResponse = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sourceAccountRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String sourceAccountId = objectMapper.readTree(sourceResponse).get("id").asText();
        
        // Step 2: Create destination account
        UUID destUserId = UUID.randomUUID();
        CreateAccountRequest destAccountRequest = new CreateAccountRequest(destUserId, Currency.USD);
        
        String destResponse = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(destAccountRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String destAccountId = objectMapper.readTree(destResponse).get("id").asText();
        
        // Step 3: Fund source account
        mockMvc.perform(post("/accounts/" + sourceAccountId + "/post")
                .param("operation", "credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 1000.00, \"currency\": \"USD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(1000.00));
        
        // Step 4: Initiate payment
        String requestId = UUID.randomUUID().toString();
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest(
            requestId,
            UUID.fromString(sourceAccountId),
            UUID.fromString(destAccountId),
            15000L, // $150.00 in minor units
            Currency.USD
        );
        
        String paymentResponse = mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.fromAccountId").value(sourceAccountId))
                .andExpect(jsonPath("$.toAccountId").value(destAccountId))
                .andExpect(jsonPath("$.amountMinor").value(15000))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String paymentId = objectMapper.readTree(paymentResponse).get("id").asText();
        
        // Step 5: Verify payment completed
        mockMvc.perform(get("/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        
        // Step 6: Verify account balances
        mockMvc.perform(get("/accounts/" + sourceAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(850.00)); // $1000 - $150

        mockMvc.perform(get("/accounts/" + destAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00)); // $150
        
        // Step 7: Verify ledger entries
        mockMvc.perform(get("/ledger/payments/" + paymentId + "/entries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].entryType").value("DEBIT"))
                .andExpect(jsonPath("$[0].accountId").value(sourceAccountId))
                .andExpect(jsonPath("$[0].amountMinor").value(15000))
                .andExpect(jsonPath("$[1].entryType").value("CREDIT"))
                .andExpect(jsonPath("$[1].accountId").value(destAccountId))
                .andExpect(jsonPath("$[1].amountMinor").value(15000));
    }

    @Test
    @WithMockUser
    void shouldHandleInsufficientFundsGracefully() throws Exception {
        // Create accounts
        UUID sourceUserId = UUID.randomUUID();
        UUID destUserId = UUID.randomUUID();
        
        CreateAccountRequest sourceRequest = new CreateAccountRequest(sourceUserId, Currency.USD);
        CreateAccountRequest destRequest = new CreateAccountRequest(destUserId, Currency.USD);
        
        String sourceResponse = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sourceRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String destResponse = mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(destRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String sourceAccountId = objectMapper.readTree(sourceResponse).get("id").asText();
        String destAccountId = objectMapper.readTree(destResponse).get("id").asText();
        
        // Try to pay without sufficient funds
        String requestId = UUID.randomUUID().toString();
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest(
            requestId,
            UUID.fromString(sourceAccountId),
            UUID.fromString(destAccountId),
            10000L, // $100.00
            Currency.USD
        );
        
        String paymentResponse = mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String paymentId = objectMapper.readTree(paymentResponse).get("id").asText();
        
        // Verify payment failed with correct reason
        mockMvc.perform(get("/payments/" + paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED_INSUFFICIENT_FUNDS"));
    }
}
