package com.minibank.accounts.api;

import com.minibank.accounts.api.dto.AccountBalanceDto;
import com.minibank.accounts.api.dto.ApiResponse;
import com.minibank.accounts.api.dto.ReserveFundsRequest;
import com.minibank.accounts.application.AccountService;
import com.minibank.accounts.domain.Account;
import com.minibank.accounts.domain.Currency;
import com.minibank.accounts.domain.Money;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountApiController {

    private final AccountService accountService;

    public AccountApiController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/{accountId}/reserve")
    public ResponseEntity<ApiResponse<Void>> reserveFunds(
            @PathVariable UUID accountId,
            @Valid @RequestBody ReserveFundsRequest request) {
        try {
            Money amount = new Money(request.getAmount(), Currency.valueOf(request.getCurrencyCode()));
            accountService.reserveFunds(accountId, amount);
            return ResponseEntity.ok(ApiResponse.success("Funds reserved successfully", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/{accountId}/credit")
    public ResponseEntity<ApiResponse<Void>> postCredit(
            @PathVariable UUID accountId,
            @Valid @RequestBody ReserveFundsRequest request) {
        try {
            Money amount = new Money(request.getAmount(), Currency.valueOf(request.getCurrencyCode()));
            accountService.postCredit(accountId, amount);
            return ResponseEntity.ok(ApiResponse.success("Credit posted successfully", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @PostMapping("/{accountId}/debit")
    public ResponseEntity<ApiResponse<Void>> postDebit(
            @PathVariable UUID accountId,
            @Valid @RequestBody ReserveFundsRequest request) {
        try {
            Money amount = new Money(request.getAmount(), Currency.valueOf(request.getCurrencyCode()));
            accountService.postDebit(accountId, amount);
            return ResponseEntity.ok(ApiResponse.success("Debit posted successfully", null));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountBalanceDto>> getAccountBalance(
            @PathVariable UUID accountId,
            @RequestParam String currencyCode) {
        try {
            Account account = accountService.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));

            Currency currency = Currency.valueOf(currencyCode);
            Money balance = account.getBalance();

            if (!balance.getCurrency().equals(currency)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Currency mismatch"));
            }

            AccountBalanceDto dto = new AccountBalanceDto(
                    currency.getCode(),
                    balance.getAmount(),
                    java.math.BigDecimal.ZERO, // Reserved amount would need separate tracking
                    balance.getAmount()
            );

            return ResponseEntity.ok(ApiResponse.success(dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Internal server error: " + e.getMessage()));
        }
    }
}