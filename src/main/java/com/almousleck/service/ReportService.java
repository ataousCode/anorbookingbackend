package com.almousleck.service;

import com.almousleck.dto.report.BookingReportRequest;
import com.almousleck.dto.report.EventReportRequest;
import com.almousleck.exception.ResourceNotFoundException;
import com.almousleck.model.Booking;
import com.almousleck.model.Event;
import com.almousleck.model.User;
import com.almousleck.repository.BookingRepository;
import com.almousleck.repository.EventRepository;
import com.almousleck.repository.UserRepository;
import com.almousleck.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public byte[] generateEventReport(UserPrincipal currentUser, EventReportRequest request) throws IOException {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        List<Event> events;
        if (request.getOrganizerOnly()) {
            events = eventRepository.findByOrganizer(user);
        } else {
            // Only admins can see all events
            if (!currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                throw new AccessDeniedException("You don't have permission to access all events");
            }
            events = eventRepository.findAll();
        }

        // Apply filters
        if (request.getStartDate() != null) {
            events = events.stream()
                    .filter(event -> event.getStartDate().isAfter(request.getStartDate()))
                    .collect(Collectors.toList());
        }

        if (request.getEndDate() != null) {
            events = events.stream()
                    .filter(event -> event.getStartDate().isBefore(request.getEndDate()))
                    .collect(Collectors.toList());
        }

        if (request.getCategoryId() != null) {
            events = events.stream()
                    .filter(event -> event.getCategory().getId().equals(request.getCategoryId()))
                    .collect(Collectors.toList());
        }

        // Generate CSV
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("Event ID", "Title", "Category", "Start Date", "End Date", "Location",
                             "Base Price", "Organizer", "Published", "Total Tickets", "Available Tickets"))) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Event event : events) {
                int totalTickets = event.getTickets().stream()
                        .mapToInt(ticket -> ticket.getTotalQuantity())
                        .sum();

                int availableTickets = event.getTickets().stream()
                        .mapToInt(ticket -> ticket.getAvailableQuantity())
                        .sum();

                csvPrinter.printRecord(
                        event.getId(),
                        event.getTitle(),
                        event.getCategory().getName(),
                        event.getStartDate().format(formatter),
                        event.getEndDate().format(formatter),
                        event.getLocation(),
                        event.getBasePrice(),
                        event.getOrganizer().getName(),
                        event.isPublished() ? "Yes" : "No",
                        totalTickets,
                        availableTickets
                );
            }

            csvPrinter.flush();
        }

        return out.toByteArray();
    }

    public byte[] generateBookingReport(UserPrincipal currentUser, BookingReportRequest request) throws IOException {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        List<Booking> bookings;
        if (request.getEventId() != null) {
            Event event = eventRepository.findById(request.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event", "id", request.getEventId()));

            // Check if user is the organizer or an admin
            if (!event.getOrganizer().getId().equals(user.getId()) &&
                    !currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                throw new AccessDeniedException("You don't have permission to access bookings for this event");
            }

            bookings = bookingRepository.findByEvent(event);
        } else if (request.getUserBookingsOnly()) {
            bookings = bookingRepository.findByUser(user);
        } else if (request.getOrganizerBookingsOnly()) {
            bookings = bookingRepository.findByEventOrganizer(user);
        } else {
            // Only admins can see all bookings
            if (!currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                throw new AccessDeniedException("You don't have permission to access all bookings");
            }
            bookings = bookingRepository.findAll();
        }

        // Apply filters
        if (request.getStartDate() != null) {
            LocalDateTime startDate = request.getStartDate();
            bookings = bookings.stream()
                    .filter(booking -> booking.getCreatedAt().isAfter(startDate))
                    .collect(Collectors.toList());
        }

        if (request.getEndDate() != null) {
            LocalDateTime endDate = request.getEndDate();
            bookings = bookings.stream()
                    .filter(booking -> booking.getCreatedAt().isBefore(endDate))
                    .collect(Collectors.toList());
        }

        if (request.getStatus() != null) {
            Booking.BookingStatus status = Booking.BookingStatus.valueOf(request.getStatus());
            bookings = bookings.stream()
                    .filter(booking -> booking.getStatus() == status)
                    .collect(Collectors.toList());
        }

        // Generate CSV
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("Booking ID", "Reference", "Event", "User", "Ticket Type", "Quantity",
                             "Total Amount", "Status", "Created At"))) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Booking booking : bookings) {
                csvPrinter.printRecord(
                        booking.getId(),
                        booking.getBookingReference(),
                        booking.getEvent().getTitle(),
                        booking.getUser().getName() + " (" + booking.getUser().getEmail() + ")",
                        booking.getTicket().getType(),
                        booking.getQuantity(),
                        booking.getTotalAmount(),
                        booking.getStatus().name(),
                        booking.getCreatedAt().format(formatter)
                );
            }

            csvPrinter.flush();
        }

        return out.toByteArray();
    }
}

