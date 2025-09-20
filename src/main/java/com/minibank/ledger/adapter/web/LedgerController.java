package com.minibank.ledger.adapter.web;

import com.minibank.ledger.adapter.web.dto.LedgerEntryResponse;
import com.minibank.ledger.application.LedgerService;
import com.minibank.ledger.domain.LedgerEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ledger")
public class LedgerController {
    private static final Logger logger = LoggerFactory.getLogger(LedgerController.class);

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/payments/{paymentId}/entries")
    public ResponseEntity<List<LedgerEntryResponse>> getPaymentEntries(@PathVariable UUID paymentId) {
        logger.debug("Getting ledger entries for payment: {}", paymentId);
        
        List<LedgerEntry> entries = ledgerService.getEntriesForPayment(paymentId);
        
        if (entries.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<LedgerEntryResponse> responses = entries.stream()
            .map(this::toResponse)
            .toList();
            
        return ResponseEntity.ok(responses);
    }

    private LedgerEntryResponse toResponse(LedgerEntry entry) {
        return new LedgerEntryResponse(
            entry.getId(),
            entry.getPaymentId(),
            entry.getAccountId(),
            entry.getEntryType(),
            entry.getAmountMinor(),
            entry.getCurrency(),
            entry.getOccurredAt()
        );
    }
}