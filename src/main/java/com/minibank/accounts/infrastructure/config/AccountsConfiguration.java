package com.minibank.accounts.infrastructure.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.minibank.accounts.adapter.persistence")
@EntityScan(basePackages = "com.minibank.accounts.adapter.persistence")
@EnableJpaAuditing
public class AccountsConfiguration {
}