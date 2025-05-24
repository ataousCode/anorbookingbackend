package com.almousleck.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private String transactionReference;
    private BigDecimal amount;
    private String status;
    private String paymentMethod;
    private String bookingReference;
    private String redirectUrl;
    private LocalDateTime createdAt;
}