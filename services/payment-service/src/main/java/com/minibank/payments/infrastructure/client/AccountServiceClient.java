package com.minibank.payments.infrastructure.client;

import com.minibank.payments.infrastructure.client.dto.AccountApiResponse;
import com.minibank.payments.infrastructure.client.dto.ReserveFundsRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

@Component
public class AccountServiceClient {

    private final WebClient webClient;
    private final String accountServiceUrl;

    public AccountServiceClient(@Value("${services.account.url}") String accountServiceUrl) {
        this.accountServiceUrl = accountServiceUrl;
        this.webClient = WebClient.builder()
                .baseUrl(accountServiceUrl)
                .build();
    }

    public AccountOperationResult reserveFunds(UUID accountId, BigDecimal amount, String currencyCode) {
        ReserveFundsRequest request = new ReserveFundsRequest(
                accountId, amount, currencyCode, UUID.randomUUID().toString()
        );

        try {
            AccountApiResponse response = webClient.post()
                    .uri("/api/v1/accounts/{accountId}/reserve", accountId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AccountApiResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            return response != null && response.isSuccess()
                    ? AccountOperationResult.success()
                    : AccountOperationResult.failure("Reserve funds failed");

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return AccountOperationResult.failure("Invalid request: " + e.getResponseBodyAsString());
            }
            return AccountOperationResult.failure("Account service error: " + e.getMessage());
        } catch (Exception e) {
            return AccountOperationResult.failure("Communication error: " + e.getMessage());
        }
    }

    public AccountOperationResult postCredit(UUID accountId, BigDecimal amount, String currencyCode) {
        ReserveFundsRequest request = new ReserveFundsRequest(
                accountId, amount, currencyCode, UUID.randomUUID().toString()
        );

        try {
            AccountApiResponse response = webClient.post()
                    .uri("/api/v1/accounts/{accountId}/credit", accountId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AccountApiResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            return response != null && response.isSuccess()
                    ? AccountOperationResult.success()
                    : AccountOperationResult.failure("Post credit failed");

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return AccountOperationResult.failure("Invalid request: " + e.getResponseBodyAsString());
            }
            return AccountOperationResult.failure("Account service error: " + e.getMessage());
        } catch (Exception e) {
            return AccountOperationResult.failure("Communication error: " + e.getMessage());
        }
    }

    public AccountOperationResult postDebit(UUID accountId, BigDecimal amount, String currencyCode) {
        ReserveFundsRequest request = new ReserveFundsRequest(
                accountId, amount, currencyCode, UUID.randomUUID().toString()
        );

        try {
            AccountApiResponse response = webClient.post()
                    .uri("/api/v1/accounts/{accountId}/debit", accountId)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(AccountApiResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            return response != null && response.isSuccess()
                    ? AccountOperationResult.success()
                    : AccountOperationResult.failure("Post debit failed");

        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return AccountOperationResult.failure("Invalid request: " + e.getResponseBodyAsString());
            }
            return AccountOperationResult.failure("Account service error: " + e.getMessage());
        } catch (Exception e) {
            return AccountOperationResult.failure("Communication error: " + e.getMessage());
        }
    }

    public static class AccountOperationResult {
        private final boolean success;
        private final String errorMessage;

        private AccountOperationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static AccountOperationResult success() {
            return new AccountOperationResult(true, null);
        }

        public static AccountOperationResult failure(String errorMessage) {
            return new AccountOperationResult(false, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
}