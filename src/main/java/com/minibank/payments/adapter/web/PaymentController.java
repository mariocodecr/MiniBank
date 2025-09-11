package com.minibank.payments.adapter.web;

import com.minibank.payments.adapter.web.dto.CreatePaymentRequest;
import com.minibank.payments.adapter.web.dto.PaymentResponse;
import com.minibank.payments.application.PaymentOrchestrationService;
import com.minibank.payments.domain.Payment;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentOrchestrationService paymentOrchestrationService;
    private final PaymentResponseMapper responseMapper;

    public PaymentController(PaymentOrchestrationService paymentOrchestrationService,
                           PaymentResponseMapper responseMapper) {
        this.paymentOrchestrationService = paymentOrchestrationService;
        this.responseMapper = responseMapper;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        logger.info("Received payment request - requestId: {}", request.getRequestId());
        
        try {
            Payment payment = paymentOrchestrationService.initiatePayment(
                request.getRequestId(),
                request.getFromAccount(),
                request.getToAccount(),
                request.getAmountMinor(),
                request.getCurrency()
            );
            
            PaymentResponse response = responseMapper.toResponse(payment);
            
            logger.info("Payment created successfully - paymentId: {}, status: {}", 
                       payment.getId(), payment.getStatus());
            
            return new ResponseEntity<>(response, HttpStatus.ACCEPTED);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid payment request: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error creating payment", e);
            throw new RuntimeException("Failed to create payment", e);
        }
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID paymentId) {
        logger.debug("Getting payment: {}", paymentId);
        
        return paymentOrchestrationService.getPayment(paymentId)
            .map(payment -> {
                PaymentResponse response = responseMapper.toResponse(payment);
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}