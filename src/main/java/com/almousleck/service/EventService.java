package com.almousleck.service;

import com.almousleck.audit.AuditLogger;
import com.almousleck.dto.event.*;
import com.almousleck.exception.BadRequestException;
import com.almousleck.exception.ResourceNotFoundException;
import com.almousleck.model.Event;
import com.almousleck.model.EventCategory;
import com.almousleck.model.Ticket;
import com.almousleck.model.User;
import com.almousleck.repository.EventCategoryRepository;
import com.almousleck.repository.EventRepository;
import com.almousleck.repository.TicketRepository;
import com.almousleck.repository.UserRepository;
import com.almousleck.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;

    @Cacheable(value = "events")
    public Page<EventSummaryResponse> getAllEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findByPublishedTrue(pageable);
        return events.map(this::convertToEventSummary);
    }

    @CacheEvict(value = {"events", "upcomingEvents", "eventsByCategory"}, allEntries = true)
    public void clearEventCache() {
        log.info("Event caches cleared successfully");
    }

    @Cacheable(value = "eventsByCategory", key = "#categoryId")
    public Page<EventSummaryResponse> getEventsByCategory(Long categoryId, Pageable pageable) {
        EventCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("EventCategory", "id", categoryId));

        Page<Event> events = eventRepository.findByPublishedTrueAndCategory(category, pageable);
        return events.map(this::convertToEventSummary);
    }

    public Page<EventSummaryResponse> searchEvents(String keyword, Pageable pageable) {
        Page<Event> events = eventRepository.searchEvents(keyword, pageable);
        return events.map(this::convertToEventSummary);
    }

    @Cacheable(value = "upcomingEvents")
    public Page<EventSummaryResponse> getUpcomingEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findUpcomingEvents(LocalDateTime.now(), pageable);
        return events.map(this::convertToEventSummary);
    }

    public EventDetailResponse getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.isPublished()) {
            throw new ResourceNotFoundException("Event", "id", eventId);
        }

        return convertToEventDetail(event);
    }

    @Transactional
    @CacheEvict(value = {"events", "upcomingEvents", "eventsByCategory"}, allEntries = true)
    public EventDetailResponse createEvent(UserPrincipal currentUser, CreateEventRequest createEventRequest) {
        User organizer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        EventCategory category = categoryRepository.findById(createEventRequest.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("EventCategory", "id", createEventRequest.getCategoryId()));

        if (!category.isActive()) {
            throw new BadRequestException("Cannot create event with inactive category");
        }

        Event event = Event.builder()
                .title(createEventRequest.getTitle())
                .description(createEventRequest.getDescription())
                .location(createEventRequest.getLocation())
                .startDate(createEventRequest.getStartDate())
                .endDate(createEventRequest.getEndDate())
                .basePrice(createEventRequest.getBasePrice())
                .imageUrl(createEventRequest.getImageUrl())
                .published(createEventRequest.isPublished())
                .category(category)
                .organizer(organizer)
                .build();

        Event savedEvent = eventRepository.save(event);

        // Create tickets for the event
        if (createEventRequest.getTickets() != null && !createEventRequest.getTickets().isEmpty()) {
            createEventRequest.getTickets().forEach(ticketRequest -> {
                Ticket ticket = Ticket.builder()
                        .type(ticketRequest.getType())
                        .price(ticketRequest.getPrice())
                        .totalQuantity(ticketRequest.getQuantity())
                        .availableQuantity(ticketRequest.getQuantity())
                        .event(savedEvent)
                        .build();

                ticketRepository.save(ticket);
            });
        } else {
            // Create a default ticket if none provided
            Ticket defaultTicket = Ticket.builder()
                    .type("General Admission")
                    .price(createEventRequest.getBasePrice())
                    .totalQuantity(100) // Default quantity
                    .availableQuantity(100)
                    .event(savedEvent)
                    .build();

            ticketRepository.save(defaultTicket);
        }


        // Log audit event
        auditLogger.logEvent("CREATE", "Event", savedEvent.getId(), currentUser,
                "Created event: " + savedEvent.getTitle());

        return convertToEventDetail(savedEvent);
    }

    @Transactional
    @CacheEvict(value = {"events", "upcomingEvents", "eventsByCategory"}, allEntries = true)
    public EventDetailResponse updateEvent(UserPrincipal currentUser, Long eventId, UpdateEventRequest updateEventRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.getOrganizer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to update this event");
        }

        if (updateEventRequest.getTitle() != null) {
            event.setTitle(updateEventRequest.getTitle());
        }

        if (updateEventRequest.getDescription() != null) {
            event.setDescription(updateEventRequest.getDescription());
        }

        if (updateEventRequest.getLocation() != null) {
            event.setLocation(updateEventRequest.getLocation());
        }

        if (updateEventRequest.getStartDate() != null) {
            event.setStartDate(updateEventRequest.getStartDate());
        }

        if (updateEventRequest.getEndDate() != null) {
            event.setEndDate(updateEventRequest.getEndDate());
        }

        if (updateEventRequest.getBasePrice() != null) {
            event.setBasePrice(updateEventRequest.getBasePrice());
        }

        if (updateEventRequest.getImageUrl() != null) {
            event.setImageUrl(updateEventRequest.getImageUrl());
        }

        if (updateEventRequest.getCategoryId() != null) {
            EventCategory category = categoryRepository.findById(updateEventRequest.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("EventCategory", "id", updateEventRequest.getCategoryId()));
            event.setCategory(category);
        }

        event.setPublished(updateEventRequest.isPublished());

        Event updatedEvent = eventRepository.save(event);

        // Log audit event
        auditLogger.logEvent("UPDATE", "Event", updatedEvent.getId(), currentUser,
                "Updated event: " + updatedEvent.getTitle());

        return convertToEventDetail(updatedEvent);
    }

    @Transactional
    @CacheEvict(value = {"events", "upcomingEvents", "eventsByCategory"}, allEntries = true)
    public void deleteEvent(UserPrincipal currentUser, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.getOrganizer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to delete this event");
        }

        // Log audit event before deletion
        auditLogger.logEvent("DELETE", "Event", event.getId(), currentUser,
                "Deleted event: " + event.getTitle());

        eventRepository.delete(event);
    }

    @Transactional
    public TicketResponse createTicket(UserPrincipal currentUser, Long eventId, CreateTicketRequest createTicketRequest) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.getOrganizer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to add tickets to this event");
        }

        Ticket ticket = Ticket.builder()
                .type(createTicketRequest.getType())
                .price(createTicketRequest.getPrice())
                .totalQuantity(createTicketRequest.getQuantity())
                .availableQuantity(createTicketRequest.getQuantity())
                .event(event)
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        // Log audit event
        auditLogger.logEvent("CREATE", "Ticket", savedTicket.getId(), currentUser,
                "Created ticket: " + savedTicket.getType() + " for event: " + event.getTitle());

        return TicketResponse.builder()
                .id(savedTicket.getId())
                .type(savedTicket.getType())
                .price(savedTicket.getPrice())
                .totalQuantity(savedTicket.getTotalQuantity())
                .availableQuantity(savedTicket.getAvailableQuantity())
                .build();
    }

    @Transactional
    public TicketResponse updateTicket(UserPrincipal currentUser, Long ticketId, UpdateTicketRequest updateTicketRequest) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        if (!ticket.getEvent().getOrganizer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to update this ticket");
        }

        if (updateTicketRequest.getType() != null) {
            ticket.setType(updateTicketRequest.getType());
        }

        if (updateTicketRequest.getPrice() != null) {
            ticket.setPrice(updateTicketRequest.getPrice());
        }

        if (updateTicketRequest.getQuantity() != null) {
            int additionalQuantity = updateTicketRequest.getQuantity() - ticket.getTotalQuantity();
            ticket.setTotalQuantity(updateTicketRequest.getQuantity());
            ticket.setAvailableQuantity(ticket.getAvailableQuantity() + additionalQuantity);

            if (ticket.getAvailableQuantity() < 0) {
                throw new BadRequestException("Cannot reduce quantity below the number of tickets already sold");
            }
        }

        Ticket updatedTicket = ticketRepository.save(ticket);

        // Log audit event
        auditLogger.logEvent("UPDATE", "Ticket", updatedTicket.getId(), currentUser,
                "Updated ticket: " + updatedTicket.getType() + " for event: " + ticket.getEvent().getTitle());

        return TicketResponse.builder()
                .id(updatedTicket.getId())
                .type(updatedTicket.getType())
                .price(updatedTicket.getPrice())
                .totalQuantity(updatedTicket.getTotalQuantity())
                .availableQuantity(updatedTicket.getAvailableQuantity())
                .build();
    }

    public List<TicketResponse> getEventTickets(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        List<Ticket> tickets = ticketRepository.findByEvent(event);

        return tickets.stream()
                .map(ticket -> TicketResponse.builder()
                        .id(ticket.getId())
                        .type(ticket.getType())
                        .price(ticket.getPrice())
                        .totalQuantity(ticket.getTotalQuantity())
                        .availableQuantity(ticket.getAvailableQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    public Page<EventSummaryResponse> getOrganizerEvents(UserPrincipal currentUser, Pageable pageable) {
        User organizer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        Page<Event> events = eventRepository.findByOrganizer(organizer, pageable);
        return events.map(this::convertToEventSummary);
    }

    @Transactional
    @CacheEvict(value = {"events", "upcomingEvents", "eventsByCategory"}, allEntries = true)
    public EventDetailResponse publishEvent(UserPrincipal currentUser, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.getOrganizer().getId().equals(currentUser.getId()) &&
                !currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("You don't have permission to publish this event");
        }

        event.setPublished(true);
        Event updatedEvent = eventRepository.save(event);

        // Log audit event
        auditLogger.logEvent("PUBLISH", "Event", updatedEvent.getId(), currentUser,
                "Published event: " + updatedEvent.getTitle());

        return convertToEventDetail(updatedEvent);
    }

    @Transactional
    @CacheEvict(value = {"events", "upcomingEvents", "eventsByCategory"}, allEntries = true)
    public EventDetailResponse unpublishEvent(UserPrincipal currentUser, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.getOrganizer().getId().equals(currentUser.getId()) &&
                !currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("You don't have permission to unpublish this event");
        }

        event.setPublished(false);
        Event updatedEvent = eventRepository.save(event);

        // Log audit event
        auditLogger.logEvent("UNPUBLISH", "Event", updatedEvent.getId(), currentUser,
                "Unpublished event: " + updatedEvent.getTitle());

        return convertToEventDetail(updatedEvent);
    }

    private EventSummaryResponse convertToEventSummary(Event event) {
        return EventSummaryResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .location(event.getLocation())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .basePrice(event.getBasePrice())
                .imageUrl(event.getImageUrl())
                .categoryId(event.getCategory().getId())
                .categoryName(event.getCategory().getName())
                .organizerId(event.getOrganizer().getId())
                .organizerName(event.getOrganizer().getName())
                .status(event.isPublished() ? "PUBLISHED" : "DRAFT")
                .build();
    }

    private EventDetailResponse convertToEventDetail(Event event) {
        List<TicketResponse> tickets = ticketRepository.findByEvent(event).stream()
                .map(ticket -> TicketResponse.builder()
                        .id(ticket.getId())
                        .type(ticket.getType())
                        .price(ticket.getPrice())
                        .totalQuantity(ticket.getTotalQuantity())
                        .availableQuantity(ticket.getAvailableQuantity())
                        .build())
                .collect(Collectors.toList());

        return EventDetailResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .basePrice(event.getBasePrice())
                .imageUrl(event.getImageUrl())
                .published(event.isPublished())
                .categoryId(event.getCategory().getId())
                .categoryName(event.getCategory().getName())
                .organizerId(event.getOrganizer().getId())
                .organizerName(event.getOrganizer().getName())
                .tickets(tickets)
                .build();
    }
}

