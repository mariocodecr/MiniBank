package com.minibank.fx.adapter.web.dto;

import com.minibank.fx.domain.FXRateProvider;
import java.time.Instant;

public class ProviderStatusResponse {
    private final String name;
    private final FXRateProvider.ProviderStatus status;
    private final int priority;
    private final boolean enabled;
    private final Instant lastUpdate;

    public ProviderStatusResponse(String name, FXRateProvider.ProviderStatus status,
                                 int priority, boolean enabled, Instant lastUpdate) {
        this.name = name;
        this.status = status;
        this.priority = priority;
        this.enabled = enabled;
        this.lastUpdate = lastUpdate;
    }

    public String getName() {
        return name;
    }

    public FXRateProvider.ProviderStatus getStatus() {
        return status;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }
}