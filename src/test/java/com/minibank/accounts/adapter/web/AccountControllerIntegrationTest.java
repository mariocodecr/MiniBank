package com.minibank.accounts.adapter.web;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.accounts.adapter.web.dto.CreateAccountRequest;
import static com.minibank.accounts.adapter.web.dto.CreateAccountRequestTestDataFactory.createCRCRequest;
import static com.minibank.accounts.adapter.web.dto.CreateAccountRequestTestDataFactory.createUSDRequest;
import com.minibank.accounts.adapter.web.dto.MoneyTransactionRequest;
import static com.minibank.accounts.adapter.web.dto.MoneyTransactionRequestTestDataFactory.createUSDRequest;
import com.minibank.accounts.domain.Currency;

@SpringBootTest
@Testcontainers
@Transactional
class AccountControllerIntegrationTest {

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
    }

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc() {
        return MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser
    void shouldCreateAccount() throws Exception {
        CreateAccountRequest request = createUSDRequest(UUID.randomUUID());
        
        mockMvc().perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(request.getUserId().toString()))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.balance").value(0.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestWhenCreatingDuplicateAccount() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateAccountRequest request = createUSDRequest(userId);
        
        // Create first account
        mockMvc().perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        
        // Try to create duplicate
        mockMvc().perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void shouldGetAccountById() throws Exception {
        // Create account first
        CreateAccountRequest createRequest = createUSDRequest(UUID.randomUUID());
        String createResponse = mockMvc().perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String accountId = objectMapper.readTree(createResponse).get("id").asText();
        
        // Get account by ID
        mockMvc().perform(get("/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(accountId))
                .andExpect(jsonPath("$.userId").value(createRequest.getUserId().toString()))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @WithMockUser
    void shouldReturnNotFoundForNonExistentAccount() throws Exception {
        mockMvc().perform(get("/accounts/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void shouldGetAccountsByUserId() throws Exception {
        UUID userId = UUID.randomUUID();
        
        // Create USD account
        CreateAccountRequest usdRequest = createUSDRequest(userId);
        mockMvc().perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usdRequest)))
                .andExpect(status().isCreated());
        
        // Create CRC account
        CreateAccountRequest crcRequest = createCRCRequest(userId);
        mockMvc().perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(crcRequest)))
                .andExpect(status().isCreated());
        
        // Get accounts by user ID
        mockMvc().perform(get("/accounts").param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$[1].userId").value(userId.toString()));
    }

    @Test
    @WithMockUser
    void shouldCreditAccount() throws Exception {
        // Create account
        CreateAccountRequest createRequest = createUSDRequest(UUID.randomUUID());
        String createResponse = mockMvc().perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String accountId = objectMapper.readTree(createResponse).get("id").asText();
        
        // Credit account
        MoneyTransactionRequest creditRequest = createUSDRequest(new BigDecimal("100.50"));
        mockMvc().perform(post("/accounts/" + accountId + "/post")
                .param("operation", "credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.50));
    }

    @Test
    @WithMockUser
    void shouldDebitAccount() throws Exception {
        // Create and fund account
        CreateAccountRequest createRequest = createUSDRequest(UUID.randomUUID());
        String createResponse = mockMvc().perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String accountId = objectMapper.readTree(createResponse).get("id").asText();
        
        // Credit account first
        MoneyTransactionRequest creditRequest = createUSDRequest(new BigDecimal("100.00"));
        mockMvc().perform(post("/accounts/" + accountId + "/post")
                .param("operation", "credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditRequest)))
                .andExpect(status().isOk());
        
        // Debit account
        MoneyTransactionRequest debitRequest = createUSDRequest(new BigDecimal("30.00"));
        mockMvc().perform(post("/accounts/" + accountId + "/post")
                .param("operation", "debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.00));
    }

    @Test
    @WithMockUser
    void shouldReserveFunds() throws Exception {
        // Create and fund account
        CreateAccountRequest createRequest = createUSDRequest(UUID.randomUUID());
        String createResponse = mockMvc().perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String accountId = objectMapper.readTree(createResponse).get("id").asText();
        
        // Credit account first
        MoneyTransactionRequest creditRequest = createUSDRequest(new BigDecimal("100.00"));
        mockMvc().perform(post("/accounts/" + accountId + "/post")
                .param("operation", "credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditRequest)))
                .andExpect(status().isOk());
        
        // Reserve funds
        MoneyTransactionRequest reserveRequest = createUSDRequest(new BigDecimal("50.00"));
        mockMvc().perform(post("/accounts/" + accountId + "/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reserveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test
    @WithMockUser
    void shouldReturnBadRequestForInvalidTransactionData() throws Exception {
        CreateAccountRequest createRequest = createUSDRequest(UUID.randomUUID());
        String createResponse = mockMvc().perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        String accountId = objectMapper.readTree(createResponse).get("id").asText();
        
        // Invalid amount (negative)
        MoneyTransactionRequest invalidRequest = new MoneyTransactionRequest(new BigDecimal("-10.00"), Currency.USD);
        mockMvc().perform(post("/accounts/" + accountId + "/post")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}