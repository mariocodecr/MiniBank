package com.minibank.accounts.adapter.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.accounts.adapter.web.dto.BalanceOperationRequest;
import com.minibank.accounts.adapter.web.dto.CreateMultiCurrencyAccountRequest;
import com.minibank.accounts.adapter.web.dto.CurrencyBalanceResponse;
import com.minibank.accounts.adapter.web.dto.ErrorResponse;
import com.minibank.accounts.adapter.web.dto.MultiCurrencyAccountResponse;
import com.minibank.accounts.adapter.web.dto.SupportedCurrencyResponse;
import com.minibank.accounts.application.CurrencyService;
import com.minibank.accounts.application.MultiCurrencyAccountService;
import com.minibank.accounts.domain.Currency;
import com.minibank.accounts.domain.CurrencyBalance;
import com.minibank.accounts.domain.MultiCurrencyAccount;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class MultiCurrencyAccountController {
    private static final Logger logger = LoggerFactory.getLogger(MultiCurrencyAccountController.class);

    private final MultiCurrencyAccountService accountService;
    private final CurrencyService currencyService;
    private final MultiCurrencyAccountMapper mapper;
    private final Counter apiRequests;

    public MultiCurrencyAccountController(MultiCurrencyAccountService accountService,
                                        CurrencyService currencyService,
                                        MultiCurrencyAccountMapper mapper,
                                        MeterRegistry meterRegistry) {
        this.accountService = accountService;
        this.currencyService = currencyService;
        this.mapper = mapper;
        this.apiRequests = Counter.builder("api.accounts.requests.total")
            .description("Total API requests to accounts service")
            .register(meterRegistry);
    }

    @PostMapping
    public ResponseEntity<MultiCurrencyAccountResponse> createAccount(@Valid @RequestBody CreateMultiCurrencyAccountRequest request) {
        apiRequests.increment();
        logger.info("Creating account for: {}", request.getEmail());
        
        MultiCurrencyAccount account = accountService.createAccount(request.getAccountHolderName(), request.getEmail());
        MultiCurrencyAccountResponse response = mapper.toResponse(account);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<MultiCurrencyAccountResponse> getAccount(@PathVariable UUID accountId) {
        apiRequests.increment();
        
        return accountService.findById(accountId)
            .map(account -> ResponseEntity.ok(mapper.toResponse(account)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{accountId}/balances")
    public ResponseEntity<List<CurrencyBalanceResponse>> getAccountBalances(@PathVariable UUID accountId) {
        apiRequests.increment();
        
        Map<Currency, CurrencyBalance> balances = accountService.getAccountBalances(accountId);
        List<CurrencyBalanceResponse> response = balances.entrySet().stream()
            .map(entry -> mapper.toBalanceResponse(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountId}/balances/{currency}")
    public ResponseEntity<CurrencyBalanceResponse> getBalance(@PathVariable UUID accountId, 
                                                            @PathVariable String currency) {
        apiRequests.increment();
        
        try {
            CurrencyBalance balance = accountService.getBalance(accountId, currency);
            Currency currencyObj = currencyService.getCurrency(currency)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported currency: " + currency));
            
            CurrencyBalanceResponse response = mapper.toBalanceResponse(currencyObj, balance);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{accountId}/currencies/{currency}")
    public ResponseEntity<MultiCurrencyAccountResponse> enableCurrency(@PathVariable UUID accountId, 
                                                                       @PathVariable String currency) {
        apiRequests.increment();
        logger.info("Enabling currency {} for account {}", currency, accountId);
        
        try {
            MultiCurrencyAccount account = accountService.enableCurrency(accountId, currency);
            MultiCurrencyAccountResponse response = mapper.toResponse(account);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request to enable currency {} for account {}: {}", currency, accountId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{accountId}/balances/{currency}/credit")
    public ResponseEntity<CurrencyBalanceResponse> creditBalance(@PathVariable UUID accountId,
                                                               @PathVariable String currency,
                                                               @Valid @RequestBody BalanceOperationRequest request) {
        apiRequests.increment();
        logger.info("Crediting {} {} to account {}", request.getAmount(), currency, accountId);
        
        try {
            long amountMinor = convertToMinorUnits(request.getAmount(), currency);
            MultiCurrencyAccount account = accountService.credit(accountId, currency, amountMinor);
            
            CurrencyBalance balance = account.getBalance(currencyService.getCurrency(currency).orElseThrow());
            Currency currencyObj = currencyService.getCurrency(currency).orElseThrow();
            CurrencyBalanceResponse response = mapper.toBalanceResponse(currencyObj, balance);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid credit request for account {}: {}", accountId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{accountId}/balances/{currency}/debit")
    public ResponseEntity<CurrencyBalanceResponse> debitBalance(@PathVariable UUID accountId,
                                                              @PathVariable String currency,
                                                              @Valid @RequestBody BalanceOperationRequest request) {
        apiRequests.increment();
        logger.info("Debiting {} {} from account {}", request.getAmount(), currency, accountId);
        
        try {
            long amountMinor = convertToMinorUnits(request.getAmount(), currency);
            MultiCurrencyAccount account = accountService.debit(accountId, currency, amountMinor);
            
            CurrencyBalance balance = account.getBalance(currencyService.getCurrency(currency).orElseThrow());
            Currency currencyObj = currencyService.getCurrency(currency).orElseThrow();
            CurrencyBalanceResponse response = mapper.toBalanceResponse(currencyObj, balance);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid debit request for account {}: {}", accountId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{accountId}/balances/{currency}/reserve")
    public ResponseEntity<CurrencyBalanceResponse> reserveFunds(@PathVariable UUID accountId,
                                                              @PathVariable String currency,
                                                              @Valid @RequestBody BalanceOperationRequest request) {
        apiRequests.increment();
        logger.info("Reserving {} {} for account {}", request.getAmount(), currency, accountId);
        
        try {
            long amountMinor = convertToMinorUnits(request.getAmount(), currency);
            MultiCurrencyAccount account = accountService.reserveFunds(accountId, currency, amountMinor);
            
            CurrencyBalance balance = account.getBalance(currencyService.getCurrency(currency).orElseThrow());
            Currency currencyObj = currencyService.getCurrency(currency).orElseThrow();
            CurrencyBalanceResponse response = mapper.toBalanceResponse(currencyObj, balance);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid reserve request for account {}: {}", accountId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{accountId}/balances/{currency}/reservations")
    public ResponseEntity<CurrencyBalanceResponse> releaseReservation(@PathVariable UUID accountId,
                                                                    @PathVariable String currency,
                                                                    @Valid @RequestBody BalanceOperationRequest request) {
        apiRequests.increment();
        logger.info("Releasing {} {} reservation for account {}", request.getAmount(), currency, accountId);
        
        try {
            long amountMinor = convertToMinorUnits(request.getAmount(), currency);
            MultiCurrencyAccount account = accountService.releaseReservation(accountId, currency, amountMinor);
            
            CurrencyBalance balance = account.getBalance(currencyService.getCurrency(currency).orElseThrow());
            Currency currencyObj = currencyService.getCurrency(currency).orElseThrow();
            CurrencyBalanceResponse response = mapper.toBalanceResponse(currencyObj, balance);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid release reservation request for account {}: {}", accountId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/currencies")
    public ResponseEntity<List<SupportedCurrencyResponse>> getSupportedCurrencies() {
        apiRequests.increment();
        
        List<Currency> currencies = currencyService.getActiveCurrencies();
        List<SupportedCurrencyResponse> response = currencies.stream()
            .map(mapper::toCurrencyResponse)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    private long convertToMinorUnits(String amount, String currencyCode) {
        Currency currency = currencyService.getCurrency(currencyCode)
            .orElseThrow(() -> new IllegalArgumentException("Unsupported currency: " + currencyCode));
        
        double doubleAmount = Double.parseDouble(amount);
        return Math.round(doubleAmount * Math.pow(10, currency.getDecimalPlaces()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Invalid request: {}", e.getMessage());
        ErrorResponse error = new ErrorResponse("INVALID_REQUEST", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        logger.error("Unexpected error: {}", e.getMessage(), e);
        ErrorResponse error = new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}