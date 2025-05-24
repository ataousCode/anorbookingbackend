package com.almousleck.service;

import com.almousleck.dto.payment.PaymentRequest;
import com.almousleck.dto.payment.PaymentResponse;
import com.almousleck.exception.BadRequestException;
import com.almousleck.exception.ResourceNotFoundException;
import com.almousleck.model.Booking;
import com.almousleck.model.PaymentTransaction;
import com.almousleck.repository.BookingRepository;
import com.almousleck.repository.PaymentTransactionRepository;
import com.almousleck.security.UserPrincipal;
import com.almousleck.utils.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Transactional
    public PaymentResponse initiatePayment(UserPrincipal currentUser, PaymentRequest paymentRequest) {
        Booking booking = bookingRepository.findByBookingReference(paymentRequest.getBookingReference())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", paymentRequest.getBookingReference()));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to make payment for this booking");
        }

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new BadRequestException("Payment can only be made for bookings in PENDING state");
        }

        // Check if payment already exists
        if (paymentTransactionRepository.findByBooking(booking).isPresent()) {
            throw new BadRequestException("Payment already initiated for this booking");
        }

        // Generate transaction reference
        String transactionReference = ReferenceGenerator.generateTransactionReference();

        // Create payment transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .transactionReference(transactionReference)
                .amount(booking.getTotalAmount())
                .status(PaymentTransaction.PaymentStatus.PENDING)
                .paymentMethod(PaymentTransaction.PaymentMethod.valueOf(paymentRequest.getPaymentMethod()))
                .booking(booking)
                .build();

        PaymentTransaction savedTransaction = paymentTransactionRepository.save(transaction);

        // In a real application, this would integrate with a payment gateway
        // For now, we'll simulate a successful payment
        log.info("Payment initiated for booking: {}, amount: {}", booking.getBookingReference(), booking.getTotalAmount());

        return PaymentResponse.builder()
                .transactionReference(savedTransaction.getTransactionReference())
                .amount(savedTransaction.getAmount())
                .status(savedTransaction.getStatus().name())
                .paymentMethod(savedTransaction.getPaymentMethod().name())
                .bookingReference(booking.getBookingReference())
                .redirectUrl("/payment/process/" + savedTransaction.getTransactionReference())
                .build();
    }

    @Transactional
    public PaymentResponse completePayment(UserPrincipal currentUser, String transactionReference) {
        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", "reference", transactionReference));

        Booking booking = transaction.getBooking();

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to complete this payment");
        }

        if (transaction.getStatus() != PaymentTransaction.PaymentStatus.PENDING) {
            throw new BadRequestException("Payment is not in PENDING state");
        }

        // In a real application, this would verify the payment with the payment gateway
        // For now, we'll simulate a successful payment
        transaction.setStatus(PaymentTransaction.PaymentStatus.COMPLETED);
        transaction.setPaymentGatewayResponse("Payment processed successfully");

        PaymentTransaction completedTransaction = paymentTransactionRepository.save(transaction);

        // Update booking status to CONFIRMED
        bookingService.confirmBooking(currentUser, booking.getBookingReference());

        log.info("Payment completed for booking: {}, amount: {}", booking.getBookingReference(), transaction.getAmount());

        return PaymentResponse.builder()
                .transactionReference(completedTransaction.getTransactionReference())
                .amount(completedTransaction.getAmount())
                .status(completedTransaction.getStatus().name())
                .paymentMethod(completedTransaction.getPaymentMethod().name())
                .bookingReference(booking.getBookingReference())
                .build();
    }

    @Transactional
    public PaymentResponse getPaymentStatus(UserPrincipal currentUser, String transactionReference) {
        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionReference(transactionReference)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", "reference", transactionReference));

        Booking booking = transaction.getBooking();

        if (!booking.getUser().getId().equals(currentUser.getId()) &&
                !booking.getEvent().getOrganizer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to view this payment");
        }

        return PaymentResponse.builder()
                .transactionReference(transaction.getTransactionReference())
                .amount(transaction.getAmount())
                .status(transaction.getStatus().name())
                .paymentMethod(transaction.getPaymentMethod().name())
                .bookingReference(booking.getBookingReference())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}

