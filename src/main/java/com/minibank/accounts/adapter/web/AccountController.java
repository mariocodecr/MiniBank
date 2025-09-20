package com.minibank.accounts.adapter.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.accounts.adapter.web.dto.AccountResponse;
import com.minibank.accounts.adapter.web.dto.CreateAccountRequest;
import com.minibank.accounts.adapter.web.dto.MoneyTransactionRequest;
import com.minibank.accounts.application.AccountService;
import com.minibank.accounts.domain.Account;
import com.minibank.accounts.domain.Money;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/accounts")
public class AccountController {
    
    private final AccountService accountService;
    private final AccountResponseMapper responseMapper;
    
    public AccountController(AccountService accountService, AccountResponseMapper responseMapper) {
        this.accountService = accountService;
        this.responseMapper = responseMapper;
    }
    
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Account account = accountService.createAccount(request.getUserId(), request.getCurrency());
        return new ResponseEntity<>(responseMapper.toResponse(account), HttpStatus.CREATED);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {
        return accountService.getAccount(id)
            .map(account -> ResponseEntity.ok(responseMapper.toResponse(account)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccountsByUserId(@RequestParam UUID userId) {
        List<Account> accounts = accountService.getAccountsByUserId(userId);
        List<AccountResponse> responses = accounts.stream()
            .map(responseMapper::toResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }
    
    @PostMapping("/{id}/reserve")
    public ResponseEntity<AccountResponse> reserveFunds(
            @PathVariable UUID id, 
            @Valid @RequestBody MoneyTransactionRequest request) {
        Money amount = Money.of(request.getAmount(), request.getCurrency());
        Account account = accountService.reserveFunds(id, amount);
        return ResponseEntity.ok(responseMapper.toResponse(account));
    }
    
    @PostMapping("/{id}/post")
    public ResponseEntity<AccountResponse> postTransaction(
            @PathVariable UUID id,
            @Valid @RequestBody MoneyTransactionRequest request,
            @RequestParam(defaultValue = "debit") String operation) {
        
        Money amount = Money.of(request.getAmount(), request.getCurrency());
        Account account;
        
        if ("credit".equalsIgnoreCase(operation)) {
            account = accountService.postCredit(id, amount);
        } else {
            account = accountService.postDebit(id, amount);
        }
        
        return ResponseEntity.ok(responseMapper.toResponse(account));
    }
}