package com.minibank.compliance.infrastructure.events;

import com.minibank.compliance.domain.AMLScreeningResult;

public interface ComplianceEventPublisher {
    void publishAMLScreeningCompleted(AMLScreeningResult result);
}