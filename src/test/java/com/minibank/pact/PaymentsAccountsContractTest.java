package com.minibank.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "accounts-service")
class PaymentsAccountsContractTest {

    @Pact(consumer = "payments-service")
    public RequestResponsePact reserveFundsContract(PactDslWithProvider builder) {
        return builder
            .given("account exists with sufficient balance")
            .uponReceiving("a request to reserve funds")
            .path("/accounts/550e8400-e29b-41d4-a716-446655440000/reserve")
            .method("POST")
            .headers("Content-Type", "application/json")
            .body("{\"amount\":100.00,\"currency\":\"USD\"}")
            .willRespondWith()
            .status(200)
            .headers("Content-Type", "application/json")
            .body("{\"id\":\"550e8400-e29b-41d4-a716-446655440000\",\"balance\":900.00}")
            .toPact();
    }

    @Pact(consumer = "payments-service")
    public RequestResponsePact postCreditContract(PactDslWithProvider builder) {
        return builder
            .given("account exists")
            .uponReceiving("a request to post credit")
            .path("/accounts/550e8400-e29b-41d4-a716-446655440001/post")
            .method("POST")
            .query("operation=credit")
            .headers("Content-Type", "application/json")
            .body("{\"amount\":100.00,\"currency\":\"USD\"}")
            .willRespondWith()
            .status(200)
            .headers("Content-Type", "application/json")
            .body("{\"id\":\"550e8400-e29b-41d4-a716-446655440001\",\"balance\":100.00}")
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "reserveFundsContract")
    void testReserveFundsContract(MockServer mockServer) {
        // Arrange
        RestTemplate restTemplate = new RestTemplate();
        String url = mockServer.getUrl() + "/accounts/550e8400-e29b-41d4-a716-446655440000/reserve";
        
        Map<String, Object> request = new HashMap<>();
        request.put("amount", 100.00);
        request.put("currency", "USD");

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody()).containsKey("balance");
        assertThat(response.getBody().get("balance")).isEqualTo(900.0);
    }

    @Test
    @PactTestFor(pactMethod = "postCreditContract")
    void testPostCreditContract(MockServer mockServer) {
        // Arrange
        RestTemplate restTemplate = new RestTemplate();
        String url = mockServer.getUrl() + "/accounts/550e8400-e29b-41d4-a716-446655440001/post?operation=credit";
        
        Map<String, Object> request = new HashMap<>();
        request.put("amount", 100.00);
        request.put("currency", "USD");

        // Act
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("id");
        assertThat(response.getBody()).containsKey("balance");
        assertThat(response.getBody().get("balance")).isEqualTo(100.0);
    }
}