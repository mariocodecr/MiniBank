package com.minibank.fx.adapter.web;

import com.minibank.fx.application.FXRateService;
import com.minibank.fx.domain.ExchangeRate;
import com.minibank.fx.domain.FXRateProvider;
import com.minibank.fx.adapter.web.dto.ExchangeRateResponse;
import com.minibank.fx.adapter.web.dto.ProviderStatusResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fx")
public class FXRateController {
    private static final Logger logger = LoggerFactory.getLogger(FXRateController.class);
    
    private final FXRateService fxRateService;
    private final Counter apiRequests;

    public FXRateController(FXRateService fxRateService, MeterRegistry meterRegistry) {
        this.fxRateService = fxRateService;
        this.apiRequests = Counter.builder("api.fx.requests.total")
            .description("Total API requests to FX service")
            .register(meterRegistry);
    }

    @GetMapping("/rates/{baseCurrency}/{quoteCurrency}")
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(@PathVariable String baseCurrency,
                                                               @PathVariable String quoteCurrency) {
        apiRequests.increment();
        logger.info("Getting exchange rate for {}/{}", baseCurrency, quoteCurrency);
        
        return fxRateService.getExchangeRate(baseCurrency.toUpperCase(), quoteCurrency.toUpperCase())
            .map(rate -> ResponseEntity.ok(toResponse(rate)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/rates")
    public ResponseEntity<List<ExchangeRateResponse>> getAllActiveRates() {
        apiRequests.increment();
        logger.info("Getting all active exchange rates");
        
        List<ExchangeRate> rates = fxRateService.getAllActiveRates();
        List<ExchangeRateResponse> response = rates.stream()
            .map(this::toResponse)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rates/{baseCurrency}/{quoteCurrency}/refresh")
    public ResponseEntity<ExchangeRateResponse> refreshRate(@PathVariable String baseCurrency,
                                                           @PathVariable String quoteCurrency) {
        apiRequests.increment();
        logger.info("Manually refreshing rate for {}/{}", baseCurrency, quoteCurrency);
        
        fxRateService.refreshRates(baseCurrency.toUpperCase(), quoteCurrency.toUpperCase());
        
        return fxRateService.getExchangeRate(baseCurrency.toUpperCase(), quoteCurrency.toUpperCase())
            .map(rate -> ResponseEntity.ok(toResponse(rate)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/rates/refresh")
    public ResponseEntity<Void> refreshAllRates() {
        apiRequests.increment();
        logger.info("Manually refreshing all rates");
        
        fxRateService.refreshAllRates();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/providers")
    public ResponseEntity<List<ProviderStatusResponse>> getProviders() {
        apiRequests.increment();
        logger.info("Getting FX provider status");
        
        List<FXRateProvider> providers = fxRateService.getProviders();
        List<ProviderStatusResponse> response = providers.stream()
            .map(this::toProviderResponse)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/providers/{providerName}/status")
    public ResponseEntity<ProviderStatusResponse> getProviderStatus(@PathVariable String providerName) {
        apiRequests.increment();
        logger.info("Getting status for provider: {}", providerName);
        
        return fxRateService.getProviders().stream()
            .filter(provider -> provider.getProviderName().equals(providerName.toUpperCase()))
            .findFirst()
            .map(this::toProviderResponse)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    private ExchangeRateResponse toResponse(ExchangeRate rate) {
        return new ExchangeRateResponse(
            rate.getId(),
            rate.getBaseCurrency(),
            rate.getQuoteCurrency(),
            rate.getMidRate(),
            rate.getBuyRate(),
            rate.getSellRate(),
            rate.getSpread(),
            rate.getProvider(),
            rate.getTimestamp(),
            rate.getValidUntil(),
            rate.isExpired()
        );
    }

    private ProviderStatusResponse toProviderResponse(FXRateProvider provider) {
        return new ProviderStatusResponse(
            provider.getProviderName(),
            provider.getStatus(),
            provider.getPriority(),
            provider.isEnabled(),
            provider.getLastUpdate()
        );
    }
}