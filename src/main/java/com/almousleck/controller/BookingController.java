package com.almousleck.controller;

import com.almousleck.dto.booking.BookingDetailResponse;
import com.almousleck.dto.booking.BookingSummaryResponse;
import com.almousleck.dto.booking.CreateBookingRequest;
import com.almousleck.security.CurrentUser;
import com.almousleck.security.UserPrincipal;
import com.almousleck.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingDetailResponse> createBooking(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateBookingRequest createBookingRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(currentUser, createBookingRequest));
    }

    @GetMapping("/{bookingReference}")
    public ResponseEntity<BookingDetailResponse> getBookingByReference(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String bookingReference) {
        return ResponseEntity.ok(bookingService.getBookingByReference(currentUser, bookingReference));
    }

    @PostMapping("/{bookingReference}/confirm")
    public ResponseEntity<BookingDetailResponse> confirmBooking(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String bookingReference) {
        return ResponseEntity.ok(bookingService.confirmBooking(currentUser, bookingReference));
    }

    @PostMapping("/{bookingReference}/cancel")
    public ResponseEntity<BookingDetailResponse> cancelBooking(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String bookingReference) {
        return ResponseEntity.ok(bookingService.cancelBooking(currentUser, bookingReference));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<Page<BookingSummaryResponse>> getUserBookings(
            @CurrentUser UserPrincipal currentUser, Pageable pageable) {
        return ResponseEntity.ok(bookingService.getUserBookings(currentUser, pageable));
    }

    @GetMapping("/organizer-bookings")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Page<BookingSummaryResponse>> getOrganizerBookings(
            @CurrentUser UserPrincipal currentUser, Pageable pageable) {
        return ResponseEntity.ok(bookingService.getOrganizerBookings(currentUser, pageable));
    }
}

