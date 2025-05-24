package com.almousleck.dto.payment;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotBlank(message = "Booking reference cannot be blank")
    private String bookingReference;

    @NotBlank(message = "Payment method cannot be blank")
    private String paymentMethod;
}

