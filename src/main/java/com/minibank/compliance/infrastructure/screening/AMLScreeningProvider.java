package com.minibank.compliance.infrastructure.screening;

import com.minibank.compliance.domain.AMLScreeningResult;
import java.util.Map;
import java.util.UUID;

public interface AMLScreeningProvider {
    String getProviderName();
    
    int getPriority();
    
    boolean isEnabled();
    
    AMLScreeningResult performScreening(UUID accountId, UUID transactionId, 
                                       AMLScreeningResult.ScreeningType screeningType,
                                       Map<String, Object> screeningData);
}