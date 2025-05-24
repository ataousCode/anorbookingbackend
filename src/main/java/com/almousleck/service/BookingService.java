package com.almousleck.service;

import com.almousleck.audit.AuditLogger;
import com.almousleck.dto.booking.BookingDetailResponse;
import com.almousleck.dto.booking.BookingSummaryResponse;
import com.almousleck.dto.booking.CreateBookingRequest;
import com.almousleck.exception.BadRequestException;
import com.almousleck.exception.ResourceNotFoundException;
import com.almousleck.model.*;
import com.almousleck.repository.BookingRepository;
import com.almousleck.repository.EventRepository;
import com.almousleck.repository.TicketRepository;
import com.almousleck.repository.UserRepository;
import com.almousleck.security.UserPrincipal;
import com.almousleck.utils.ReferenceGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final AuditLogger auditLogger;

    @Transactional
    public BookingDetailResponse createBooking(UserPrincipal currentUser, CreateBookingRequest createBookingRequest) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        Ticket ticket = ticketRepository.findByIdWithLock(createBookingRequest.getTicketId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", createBookingRequest.getTicketId()));

        Event event = ticket.getEvent();

        if (!event.isPublished()) {
            throw new BadRequestException("Cannot book tickets for an unpublished event");
        }

        if (event.getStartDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot book tickets for a past event");
        }

        if (ticket.getAvailableQuantity() < createBookingRequest.getQuantity()) {
            throw new BadRequestException("Not enough tickets available");
        }

        // Calculate total amount
        BigDecimal totalAmount = ticket.getPrice().multiply(BigDecimal.valueOf(createBookingRequest.getQuantity()));

        // Generate unique booking reference
        String bookingReference = ReferenceGenerator.generateBookingReference();

        // Create booking
        Booking booking = Booking.builder()
                .bookingReference(bookingReference)
                .quantity(createBookingRequest.getQuantity())
                .totalAmount(totalAmount)
                .status(Booking.BookingStatus.PENDING)
                .user(user)
                .ticket(ticket)
                .event(event)
                .build();

        // Update ticket availability
        ticket.setAvailableQuantity(ticket.getAvailableQuantity() - createBookingRequest.getQuantity());
        ticketRepository.save(ticket);

        Booking savedBooking = bookingRepository.save(booking);

        // Send confirmation email
        emailService.sendBookingConfirmation(user.getEmail(), user.getName(), savedBooking);

        // Create notification for user
        notificationService.createNotification(
                user,
                "Booking Created",
                "Your booking for " + event.getTitle() + " has been created. Reference: " + bookingReference,
                Notification.NotificationType.SUCCESS
        );

        // Create notification for organizer
        notificationService.createNotification(
                event.getOrganizer(),
                "New Booking",
                "A new booking has been made for your event " + event.getTitle() + ". Reference: " + bookingReference,
                Notification.NotificationType.INFO
        );

        // Log audit event
        auditLogger.logEvent("CREATE", "Booking", savedBooking.getId(), currentUser,
                "Created booking: " + savedBooking.getBookingReference() + " for event: " + event.getTitle());

        return convertToBookingDetail(savedBooking);
    }

    @Transactional
    public BookingDetailResponse confirmBooking(UserPrincipal currentUser, String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", bookingReference));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to confirm this booking");
        }

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new BadRequestException("Booking is not in PENDING state");
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        Booking confirmedBooking = bookingRepository.save(booking);

        // Create notification for user
        notificationService.createNotification(
                booking.getUser(),
                "Booking Confirmed",
                "Your booking for " + booking.getEvent().getTitle() + " has been confirmed. Reference: " + bookingReference,
                Notification.NotificationType.SUCCESS
        );

        // Log audit event
        auditLogger.logEvent("CONFIRM", "Booking", confirmedBooking.getId(), currentUser,
                "Confirmed booking: " + confirmedBooking.getBookingReference());

        return convertToBookingDetail(confirmedBooking);
    }

    @Transactional
    public BookingDetailResponse cancelBooking(UserPrincipal currentUser, String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", bookingReference));

        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to cancel this booking");
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }

        // Check if the event has already started
        if (booking.getEvent().getStartDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot cancel booking for an event that has already started");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);

        // Return tickets to available pool
        Ticket ticket = booking.getTicket();
        ticket.setAvailableQuantity(ticket.getAvailableQuantity() + booking.getQuantity());
        ticketRepository.save(ticket);

        Booking cancelledBooking = bookingRepository.save(booking);

        // Send cancellation email
        emailService.sendBookingCancellation(booking.getUser().getEmail(), booking.getUser().getName(), cancelledBooking);

        // Create notification for user
        notificationService.createNotification(
                booking.getUser(),
                "Booking Cancelled",
                "Your booking for " + booking.getEvent().getTitle() + " has been cancelled. Reference: " + bookingReference,
                Notification.NotificationType.INFO
        );

        // Create notification for organizer
        notificationService.createNotification(
                booking.getEvent().getOrganizer(),
                "Booking Cancelled",
                "A booking for your event " + booking.getEvent().getTitle() + " has been cancelled. Reference: " + bookingReference,
                Notification.NotificationType.INFO
        );

        // Log audit event
        auditLogger.logEvent("CANCEL", "Booking", cancelledBooking.getId(), currentUser,
                "Cancelled booking: " + cancelledBooking.getBookingReference());

        return convertToBookingDetail(cancelledBooking);
    }

    public BookingDetailResponse getBookingByReference(UserPrincipal currentUser, String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "reference", bookingReference));

        if (!booking.getUser().getId().equals(currentUser.getId()) &&
                !booking.getEvent().getOrganizer().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You don't have permission to view this booking");
        }

        return convertToBookingDetail(booking);
    }

    public Page<BookingSummaryResponse> getUserBookings(UserPrincipal currentUser, Pageable pageable) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        Page<Booking> bookings = bookingRepository.findByUser(user, pageable);
        return bookings.map(this::convertToBookingSummary);
    }

    public Page<BookingSummaryResponse> getOrganizerBookings(UserPrincipal currentUser, Pageable pageable) {
        Page<Booking> bookings = bookingRepository.findByOrganizerId(currentUser.getId(), pageable);
        return bookings.map(this::convertToBookingSummary);
    }

    private BookingSummaryResponse convertToBookingSummary(Booking booking) {
        return BookingSummaryResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .eventTitle(booking.getEvent().getTitle())
                .eventDate(booking.getEvent().getStartDate())
                .ticketType(booking.getTicket().getType())
                .quantity(booking.getQuantity())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private BookingDetailResponse convertToBookingDetail(Booking booking) {
        return BookingDetailResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .eventId(booking.getEvent().getId())
                .eventTitle(booking.getEvent().getTitle())
                .eventLocation(booking.getEvent().getLocation())
                .eventDate(booking.getEvent().getStartDate())
                .ticketId(booking.getTicket().getId())
                .ticketType(booking.getTicket().getType())
                .quantity(booking.getQuantity())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus().name())
                .userName(booking.getUser().getName())
                .userEmail(booking.getUser().getEmail())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}

