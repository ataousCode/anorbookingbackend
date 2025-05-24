package com.almousleck.controller;

import com.almousleck.dto.payment.PaymentRequest;
import com.almousleck.dto.payment.PaymentResponse;
import com.almousleck.security.CurrentUser;
import com.almousleck.security.UserPrincipal;
import com.almousleck.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    // I implemented this payment and notification service just for testing:
    // real implementation must be done in the upcoming version

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    public ResponseEntity<PaymentResponse> initiatePayment(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody PaymentRequest paymentRequest) {
        return ResponseEntity.ok(paymentService.initiatePayment(currentUser, paymentRequest));
    }

    @PostMapping("/complete/{transactionReference}")
    public ResponseEntity<PaymentResponse> completePayment(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String transactionReference) {
        return ResponseEntity.ok(paymentService.completePayment(currentUser, transactionReference));
    }

    @GetMapping("/{transactionReference}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String transactionReference) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(currentUser, transactionReference));
    }
}

