package com.minibank.ledger.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.minibank.ledger.adapter.persistence")
@EntityScan(basePackages = "com.minibank.ledger.adapter.persistence")
@EnableJpaAuditing
public class LedgerConfiguration {
}