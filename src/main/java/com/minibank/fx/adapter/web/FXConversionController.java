package com.minibank.fx.adapter.web;

import com.minibank.fx.application.FXConversionService;
import com.minibank.fx.domain.FXConversion;
import com.minibank.fx.adapter.web.dto.ConversionRequest;
import com.minibank.fx.adapter.web.dto.ConversionResponse;
import com.minibank.fx.adapter.web.dto.ConversionQuoteResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/fx/conversions")
public class FXConversionController {
    private static final Logger logger = LoggerFactory.getLogger(FXConversionController.class);
    
    private final FXConversionService conversionService;
    private final Counter apiRequests;

    public FXConversionController(FXConversionService conversionService, MeterRegistry meterRegistry) {
        this.conversionService = conversionService;
        this.apiRequests = Counter.builder("api.fx.conversions.requests.total")
            .description("Total API requests to FX conversions service")
            .register(meterRegistry);
    }

    @PostMapping
    public ResponseEntity<ConversionResponse> convertAmount(@Valid @RequestBody ConversionRequest request) {
        apiRequests.increment();
        logger.info("Processing FX conversion request: {} {} to {} for account {}", 
                   request.getFromAmountMinor(), request.getFromCurrency(), 
                   request.getToCurrency(), request.getAccountId());
        
        Optional<FXConversion> conversion = conversionService.convertAmount(
            request.getAccountId(),
            request.getFromCurrency().toUpperCase(),
            request.getToCurrency().toUpperCase(),
            request.getFromAmountMinor(),
            request.getCorrelationId()
        );
        
        return conversion
            .map(c -> ResponseEntity.ok(toResponse(c)))
            .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("/quote/{fromCurrency}/{toCurrency}")
    public ResponseEntity<ConversionQuoteResponse> getConversionQuote(@PathVariable String fromCurrency,
                                                                     @PathVariable String toCurrency,
                                                                     @RequestParam long fromAmountMinor) {
        apiRequests.increment();
        logger.info("Getting conversion quote for {} {} to {}", fromAmountMinor, fromCurrency, toCurrency);
        
        if (fromAmountMinor <= 0) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<Long> convertedAmount = conversionService.calculateConvertedAmount(
            fromCurrency.toUpperCase(), toCurrency.toUpperCase(), fromAmountMinor
        );
        
        Optional<BigDecimal> rate = conversionService.getConversionRate(
            fromCurrency.toUpperCase(), toCurrency.toUpperCase()
        );
        
        if (convertedAmount.isEmpty() || rate.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        ConversionQuoteResponse response = new ConversionQuoteResponse(
            fromCurrency.toUpperCase(),
            toCurrency.toUpperCase(),
            fromAmountMinor,
            convertedAmount.get(),
            rate.get()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<ConversionResponse>> getAccountConversions(@PathVariable UUID accountId) {
        apiRequests.increment();
        logger.info("Getting conversions for account: {}", accountId);
        
        List<FXConversion> conversions = conversionService.getConversionsForAccount(accountId);
        List<ConversionResponse> response = conversions.stream()
            .map(this::toResponse)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}/{fromCurrency}/{toCurrency}")
    public ResponseEntity<List<ConversionResponse>> getAccountConversionsByPair(@PathVariable UUID accountId,
                                                                               @PathVariable String fromCurrency,
                                                                               @PathVariable String toCurrency) {
        apiRequests.increment();
        logger.info("Getting conversions for account {} and pair {}/{}", accountId, fromCurrency, toCurrency);
        
        List<FXConversion> conversions = conversionService.getConversionsForAccountAndPair(
            accountId, fromCurrency.toUpperCase(), toCurrency.toUpperCase()
        );
        List<ConversionResponse> response = conversions.stream()
            .map(this::toResponse)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<List<ConversionResponse>> getConversionsByCorrelation(@PathVariable String correlationId) {
        apiRequests.increment();
        logger.info("Getting conversions for correlation ID: {}", correlationId);
        
        List<FXConversion> conversions = conversionService.getConversionsForCorrelation(correlationId);
        List<ConversionResponse> response = conversions.stream()
            .map(this::toResponse)
            .toList();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{conversionId}")
    public ResponseEntity<ConversionResponse> getConversion(@PathVariable UUID conversionId) {
        apiRequests.increment();
        logger.info("Getting conversion: {}", conversionId);
        
        return conversionService.getConversion(conversionId)
            .map(conversion -> ResponseEntity.ok(toResponse(conversion)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/supported/{fromCurrency}/{toCurrency}")
    public ResponseEntity<Boolean> isConversionSupported(@PathVariable String fromCurrency,
                                                        @PathVariable String toCurrency) {
        apiRequests.increment();
        logger.info("Checking if conversion is supported: {}/{}", fromCurrency, toCurrency);
        
        boolean supported = conversionService.isConversionSupported(
            fromCurrency.toUpperCase(), toCurrency.toUpperCase()
        );
        
        return ResponseEntity.ok(supported);
    }

    private ConversionResponse toResponse(FXConversion conversion) {
        return new ConversionResponse(
            conversion.getId(),
            conversion.getAccountId(),
            conversion.getFromCurrency(),
            conversion.getToCurrency(),
            conversion.getFromAmountMinor(),
            conversion.getToAmountMinor(),
            conversion.getExchangeRate(),
            conversion.getEffectiveRate(),
            conversion.getSpread(),
            conversion.getProvider(),
            conversion.getTimestamp(),
            conversion.getCorrelationId()
        );
    }
}