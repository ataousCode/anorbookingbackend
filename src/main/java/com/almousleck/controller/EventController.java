package com.almousleck.controller;

import com.almousleck.dto.auth.ApiResponse;
import com.almousleck.dto.event.*;
import com.almousleck.security.CurrentUser;
import com.almousleck.security.UserPrincipal;
import com.almousleck.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventSummaryResponse>> getAllEvents(Pageable pageable) {
        return ResponseEntity.ok(eventService.getAllEvents(pageable));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<EventSummaryResponse>> getEventsByCategory(
            @PathVariable Long categoryId, Pageable pageable) {
        return ResponseEntity.ok(eventService.getEventsByCategory(categoryId, pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<EventSummaryResponse>> searchEvents(
            @RequestParam String keyword, Pageable pageable) {
        return ResponseEntity.ok(eventService.searchEvents(keyword, pageable));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<Page<EventSummaryResponse>> getUpcomingEvents(Pageable pageable) {
        return ResponseEntity.ok(eventService.getUpcomingEvents(pageable));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDetailResponse> getEventById(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEventById(eventId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventDetailResponse> createEvent(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody CreateEventRequest createEventRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(currentUser, createEventRequest));
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<EventDetailResponse> updateEvent(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long eventId,
            @Valid @RequestBody UpdateEventRequest updateEventRequest) {
        return ResponseEntity.ok(eventService.updateEvent(currentUser, eventId, updateEventRequest));
    }

    @DeleteMapping("/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse> deleteEvent(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long eventId) {
        eventService.deleteEvent(currentUser, eventId);
        return ResponseEntity.ok(new ApiResponse(true, "Event deleted successfully"));
    }

    @GetMapping("/{eventId}/tickets")
    public ResponseEntity<List<TicketResponse>> getEventTickets(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEventTickets(eventId));
    }

    @PostMapping("/{eventId}/tickets")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<TicketResponse> createTicket(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long eventId,
            @Valid @RequestBody CreateTicketRequest createTicketRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createTicket(currentUser, eventId, createTicketRequest));
    }

    @PutMapping("/tickets/{ticketId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<TicketResponse> updateTicket(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketRequest updateTicketRequest) {
        return ResponseEntity.ok(eventService.updateTicket(currentUser, ticketId, updateTicketRequest));
    }

    @GetMapping("/my-events")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Page<EventSummaryResponse>> getOrganizerEvents(
            @CurrentUser UserPrincipal currentUser, Pageable pageable) {
        return ResponseEntity.ok(eventService.getOrganizerEvents(currentUser, pageable));
    }
}

