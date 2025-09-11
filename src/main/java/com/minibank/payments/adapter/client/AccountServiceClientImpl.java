package com.minibank.payments.adapter.client;

import com.minibank.accounts.domain.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountServiceClientImpl implements AccountServiceClient {
    private static final Logger logger = LoggerFactory.getLogger(AccountServiceClientImpl.class);

    // For M1, we'll implement this as in-process calls to the accounts service
    // In M2, this would be HTTP calls or message-based communication
    
    private final com.minibank.accounts.application.AccountService accountService;
    
    public AccountServiceClientImpl(com.minibank.accounts.application.AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public ReserveResult reserveFunds(UUID accountId, Money amount) {
        try {
            logger.debug("Reserving funds for account: {}, amount: {}", accountId, amount);
            accountService.reserveFunds(accountId, amount);
            return ReserveResult.success();
        } catch (IllegalArgumentException e) {
            logger.warn("Reserve funds failed for account {}: {}", accountId, e.getMessage());
            return ReserveResult.failure(e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("Reserve funds failed for account {}: {}", accountId, e.getMessage());
            return ReserveResult.failure(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error reserving funds for account {}", accountId, e);
            return ReserveResult.failure("System error: " + e.getMessage());
        }
    }

    @Override
    public PostResult postCredit(UUID accountId, Money amount) {
        try {
            logger.debug("Posting credit for account: {}, amount: {}", accountId, amount);
            accountService.postCredit(accountId, amount);
            return PostResult.success();
        } catch (IllegalArgumentException e) {
            logger.warn("Post credit failed for account {}: {}", accountId, e.getMessage());
            return PostResult.failure(e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("Post credit failed for account {}: {}", accountId, e.getMessage());
            return PostResult.failure(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error posting credit for account {}", accountId, e);
            return PostResult.failure("System error: " + e.getMessage());
        }
    }

    @Override
    public PostResult postDebit(UUID accountId, Money amount) {
        try {
            logger.debug("Posting debit for account: {}, amount: {}", accountId, amount);
            accountService.postDebit(accountId, amount);
            return PostResult.success();
        } catch (IllegalArgumentException e) {
            logger.warn("Post debit failed for account {}: {}", accountId, e.getMessage());
            return PostResult.failure(e.getMessage());
        } catch (IllegalStateException e) {
            logger.warn("Post debit failed for account {}: {}", accountId, e.getMessage());
            return PostResult.failure(e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error posting debit for account {}", accountId, e);
            return PostResult.failure("System error: " + e.getMessage());
        }
    }
}