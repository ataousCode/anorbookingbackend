package com.almousleck.service;

import com.almousleck.dto.statistics.EventStatisticsResponse;
import com.almousleck.dto.statistics.OrganizerStatisticsResponse;
import com.almousleck.exception.ResourceNotFoundException;
import com.almousleck.model.Booking;
import com.almousleck.model.Event;
import com.almousleck.model.User;
import com.almousleck.repository.BookingRepository;
import com.almousleck.repository.EventRepository;
import com.almousleck.repository.TicketRepository;
import com.almousleck.repository.UserRepository;
import com.almousleck.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public EventStatisticsResponse getEventStatistics(UserPrincipal currentUser, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        // Check if user is the organizer or an admin
        if (!event.getOrganizer().getId().equals(currentUser.getId()) &&
                !currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("You don't have permission to view statistics for this event");
        }

        Long totalBookings = bookingRepository.countConfirmedBookingsByEvent(event);
        Integer totalTicketsAvailable = ticketRepository.countAvailableTicketsByEvent(eventId);
        Integer totalTicketsSold = event.getTickets().stream()
                .mapToInt(ticket -> ticket.getTotalQuantity() - ticket.getAvailableQuantity())
                .sum();

        BigDecimal totalRevenue = bookingRepository.findByEventAndStatus(event, Booking.BookingStatus.CONFIRMED)
                .stream()
                .map(Booking::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return EventStatisticsResponse.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .totalBookings(totalBookings)
                .totalTicketsAvailable(totalTicketsAvailable)
                .totalTicketsSold(totalTicketsSold)
                .totalRevenue(totalRevenue)
                .build();
    }

    public OrganizerStatisticsResponse getOrganizerStatistics(UserPrincipal currentUser) {
        User organizer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        List<Event> organizerEvents = eventRepository.findByOrganizer(organizer);

        long totalEvents = organizerEvents.size();
        long publishedEvents = organizerEvents.stream().filter(Event::isPublished).count();
        long upcomingEvents = eventRepository.findUpcomingEventsByOrganizer(organizer, LocalDateTime.now()).size();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        long totalBookings = 0;

        for (Event event : organizerEvents) {
            List<Booking> confirmedBookings = bookingRepository.findByEventAndStatus(event, Booking.BookingStatus.CONFIRMED);
            totalBookings += confirmedBookings.size();

            for (Booking booking : confirmedBookings) {
                totalRevenue = totalRevenue.add(booking.getTotalAmount());
            }
        }

        List<EventStatisticsResponse> topEvents = organizerEvents.stream()
                .limit(5)
                .map(event -> {
                    Long eventBookings = bookingRepository.countConfirmedBookingsByEvent(event);
                    Integer ticketsSold = event.getTickets().stream()
                            .mapToInt(ticket -> ticket.getTotalQuantity() - ticket.getAvailableQuantity())
                            .sum();

                    BigDecimal eventRevenue = bookingRepository.findByEventAndStatus(event, Booking.BookingStatus.CONFIRMED)
                            .stream()
                            .map(Booking::getTotalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return EventStatisticsResponse.builder()
                            .eventId(event.getId())
                            .eventTitle(event.getTitle())
                            .totalBookings(eventBookings)
                            .totalTicketsSold(ticketsSold)
                            .totalRevenue(eventRevenue)
                            .build();
                })
                .collect(Collectors.toList());

        return OrganizerStatisticsResponse.builder()
                .organizerId(organizer.getId())
                .organizerName(organizer.getName())
                .totalEvents(totalEvents)
                .publishedEvents(publishedEvents)
                .upcomingEvents(upcomingEvents)
                .totalBookings(totalBookings)
                .totalRevenue(totalRevenue)
                .topEvents(topEvents)
                .build();
    }
}

