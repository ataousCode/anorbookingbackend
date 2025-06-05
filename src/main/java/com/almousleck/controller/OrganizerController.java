package com.almousleck.controller;

import com.almousleck.dto.booking.BookingSummaryResponse;
import com.almousleck.dto.event.EventSummaryResponse;
import com.almousleck.dto.organizer.*;
import com.almousleck.security.CurrentUser;
import com.almousleck.security.UserPrincipal;
import com.almousleck.service.OrganizerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/organizer")
@RequiredArgsConstructor
public class OrganizerController {

    private final OrganizerService organizerService;

    @PostMapping("/application")
    public ResponseEntity<OrganizerApplicationResponse> applyForOrganizerRole(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody OrganizerApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizerService.applyForOrganizerRole(currentUser, request));
    }

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(organizerService.getDashboardStats(currentUser));
    }

    @GetMapping("/application/status")
    public ResponseEntity<OrganizerApplicationResponse> getApplicationStatus(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(organizerService.getApplicationStatus(currentUser));
    }

    @GetMapping("/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrganizerApplicationResponse>> getPendingApplications(Pageable pageable) {
        return ResponseEntity.ok(organizerService.getPendingApplications(pageable));
    }

    @PutMapping("/application/{applicationId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizerApplicationResponse> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody OrganizerApplicationStatusRequest request) {
        return ResponseEntity.ok(organizerService.updateApplicationStatus(applicationId, request));
    }

    // New method

    @GetMapping("/profile")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<OrganizerProfileResponse> getProfile(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(organizerService.getOrganizerProfile(currentUser));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<OrganizerProfileResponse> updateProfile(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody OrganizerProfileUpdateRequest request) {
        return ResponseEntity.ok(organizerService.updateOrganizerProfile(currentUser, request));
    }

    // NEW ENDPOINTS - Event Management
    @GetMapping("/events")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Page<EventSummaryResponse>> getOrganizerEvents(
            @CurrentUser UserPrincipal currentUser,
            Pageable pageable) {
        return ResponseEntity.ok(organizerService.getOrganizerEvents(currentUser, pageable));
    }

    @GetMapping("/events/status/{status}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Page<EventSummaryResponse>> getEventsByStatus(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable String status,
            Pageable pageable) {
        return ResponseEntity.ok(organizerService.getEventsByStatus(currentUser, status, pageable));
    }

    // NEW ENDPOINTS - Booking Management
    @GetMapping("/events/{eventId}/bookings")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Page<BookingSummaryResponse>> getEventBookings(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long eventId,
            Pageable pageable) {
        return ResponseEntity.ok(organizerService.getEventBookings(currentUser, eventId, pageable));
    }

    @GetMapping("/bookings/recent")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<Page<BookingSummaryResponse>> getRecentBookings(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(organizerService.getRecentBookings(currentUser, limit));
    }

    // NEW ENDPOINTS - Analytics
    @GetMapping("/analytics/revenue")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<AnalyticsResponse> getRevenueAnalytics(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(organizerService.getRevenueAnalytics(currentUser));
    }

    @GetMapping("/analytics/bookings")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<AnalyticsResponse> getBookingAnalytics(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(organizerService.getBookingAnalytics(currentUser));
    }
}

