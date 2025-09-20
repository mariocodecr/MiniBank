package com.minibank.treasury.infrastructure.events;

import com.minibank.treasury.domain.CurrencyExposure;
import com.minibank.treasury.domain.ExposureAlert;
import java.math.BigDecimal;
import java.util.Map;

public interface TreasuryEventPublisher {
    void publishExposureAlert(ExposureAlert alert);
    
    void publishAlertAcknowledged(ExposureAlert alert);
    
    void publishAlertResolved(ExposureAlert alert);
    
    void publishExposureReport(Map<com.minibank.accounts.domain.Currency, CurrencyExposure> exposures, 
                              BigDecimal totalUSDExposure);
}