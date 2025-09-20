package com.minibank.accounts.application;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minibank.accounts.adapter.persistence.SupportedCurrencyEntity;
import com.minibank.accounts.adapter.persistence.SupportedCurrencyJpaRepository;
import com.minibank.accounts.domain.Currency;

@Service
@Transactional(readOnly = true)
public class CurrencyService {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);

    private final SupportedCurrencyJpaRepository currencyRepository;

    public CurrencyService(SupportedCurrencyJpaRepository currencyRepository) {
        this.currencyRepository = currencyRepository;
    }

    @Cacheable("currencies")
    public Optional<Currency> getCurrency(String currencyCode) {
        return currencyRepository.findByCurrencyCodeAndIsActiveTrue(currencyCode)
            .map(this::toCurrency);
    }

    @Cacheable("activeCurrencies")
    public List<Currency> getActiveCurrencies() {
        return currencyRepository.findByIsActiveTrueOrderByCurrencyCode()
            .stream()
            .map(this::toCurrency)
            .collect(Collectors.toList());
    }

    @Cacheable("activeCurrencyCodes")
    public List<String> getActiveCurrencyCodes() {
        return currencyRepository.findActiveCurrencyCodes();
    }

    public boolean isCurrencySupported(String currencyCode) {
        return currencyRepository.existsByCurrencyCodeAndIsActiveTrue(currencyCode);
    }

    @Transactional
    public Currency addOrUpdateCurrency(String currencyCode, String currencyName, 
                                       int decimalPlaces, String symbol, boolean isActive) {
        logger.info("Adding/updating currency: {} - {}", currencyCode, currencyName);
        
        SupportedCurrencyEntity entity = currencyRepository.findById(currencyCode)
            .orElse(new SupportedCurrencyEntity());
        
        entity.setCurrencyCode(currencyCode);
        entity.setCurrencyName(currencyName);
        entity.setDecimalPlaces(decimalPlaces);
        entity.setSymbol(symbol);
        entity.setIsActive(isActive);
        entity.setMinimumAmountMinor(1L); // Default minimum amount
        
        SupportedCurrencyEntity saved = currencyRepository.save(entity);
        logger.info("Successfully added/updated currency: {}", currencyCode);
        
        return toCurrency(saved);
    }

    @Transactional
    public void deactivateCurrency(String currencyCode) {
        logger.info("Deactivating currency: {}", currencyCode);
        
        Optional<SupportedCurrencyEntity> entity = currencyRepository.findById(currencyCode);
        if (entity.isPresent()) {
            SupportedCurrencyEntity currency = entity.get();
            currency.setIsActive(false);
            currencyRepository.save(currency);
            logger.info("Successfully deactivated currency: {}", currencyCode);
        }
    }

    private Currency toCurrency(SupportedCurrencyEntity entity) {
        return Currency.valueOf(entity.getCurrencyCode());
    }
}