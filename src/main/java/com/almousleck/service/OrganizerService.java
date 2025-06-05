package com.almousleck.service;

import com.almousleck.dto.booking.BookingSummaryResponse;
import com.almousleck.dto.event.EventSummaryResponse;
import com.almousleck.dto.organizer.*;
import com.almousleck.exception.BadRequestException;
import com.almousleck.exception.ResourceNotFoundException;
import com.almousleck.model.*;
import com.almousleck.repository.*;
import com.almousleck.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrganizerService {

    private final OrganizerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public OrganizerApplicationResponse applyForOrganizerRole(UserPrincipal currentUser, OrganizerApplicationRequest request) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        // Check if user already has an active application
        if (applicationRepository.existsByUserAndStatus(user, OrganizerApplication.ApplicationStatus.PENDING)) {
            throw new BadRequestException("You already have a pending application");
        }

        // Check if user is already an organizer
        boolean isOrganizer = user.getRoles().stream()
                .anyMatch(role -> role.getName() == Role.RoleName.ROLE_ORGANIZER);

        if (isOrganizer) {
            throw new BadRequestException("You are already an organizer");
        }

        OrganizerApplication application = OrganizerApplication.builder()
                .description(request.getDescription())
                .companyName(request.getCompanyName())
                .website(request.getWebsite())
                .socialMediaLinks(request.getSocialMediaLinks())
                .status(OrganizerApplication.ApplicationStatus.PENDING)
                .user(user)
                .build();

        OrganizerApplication savedApplication = applicationRepository.save(application);

        return convertToApplicationResponse(savedApplication);
    }

    public OrganizerApplicationResponse getApplicationStatus(UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        OrganizerApplication application = applicationRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizerApplication", "userId", user.getId()));

        return convertToApplicationResponse(application);
    }

    public Page<OrganizerApplicationResponse> getPendingApplications(Pageable pageable) {
        Page<OrganizerApplication> applications = applicationRepository.findByStatus(
                OrganizerApplication.ApplicationStatus.PENDING, pageable);

        return applications.map(this::convertToApplicationResponse);
    }

    @Transactional
    public OrganizerApplicationResponse updateApplicationStatus(Long applicationId, OrganizerApplicationStatusRequest request) {
        OrganizerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizerApplication", "id", applicationId));

        application.setStatus(OrganizerApplication.ApplicationStatus.valueOf(request.getStatus()));

        if (request.getAdminFeedback() != null) {
            application.setAdminFeedback(request.getAdminFeedback());
        }

        OrganizerApplication updatedApplication = applicationRepository.save(application);

        if (updatedApplication.getStatus() == OrganizerApplication.ApplicationStatus.APPROVED) {
            User user = updatedApplication.getUser();
            Role organizerRole = roleRepository.findByName(Role.RoleName.ROLE_ORGANIZER)
                    .orElseThrow(() -> new RuntimeException("Error: Organizer Role not found."));

            user.getRoles().add(organizerRole);
            userRepository.save(user);

            // Send approval notification
            emailService.sendOrganizerApprovalNotification(user.getEmail(), user.getName());
        } else if (updatedApplication.getStatus() == OrganizerApplication.ApplicationStatus.REJECTED) {
            // Send rejection notification
            emailService.sendOrganizerRejectionNotification(
                    updatedApplication.getUser().getEmail(),
                    updatedApplication.getUser().getName(),
                    updatedApplication.getAdminFeedback());
        }

        return convertToApplicationResponse(updatedApplication);
    }

    public Map<String, Object> getDashboardStats(UserPrincipal currentUser) {
        User user = getUserById(currentUser.getId());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents", eventRepository.countByOrganizerId(user.getId()));
        stats.put("totalBookings", bookingRepository.countByEventOrganizerId(user.getId()));
        stats.put("totalRevenue", bookingRepository.sumTotalAmountByEventOrganizerId(user.getId()));
        stats.put("pendingBookings", bookingRepository.countByEventOrganizerIdAndStatus(user.getId(), Booking.BookingStatus.PENDING));

        return stats;
    }

    public Page<EventSummaryResponse> getOrganizerEvents(UserPrincipal currentUser, Pageable pageable) {
        User user = getUserById(currentUser.getId());
        Page<Event> events = eventRepository.findByOrganizerId(user.getId(), pageable);
        return events.map(this::convertToEventSummaryResponse);
    }

    public Page<BookingSummaryResponse> getRecentBookings(UserPrincipal currentUser, int limit) {
        User user = getUserById(currentUser.getId());
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Booking> bookings = bookingRepository.findByEventOrganizerId(user.getId(), pageable);
        return bookings.map(this::convertToBookingSummaryResponse);
    }

    public OrganizerProfileResponse getOrganizerProfile(UserPrincipal currentUser) {
        User user = getUserById(currentUser.getId());

        // Get organizer application data
        OrganizerApplication application = applicationRepository.findByUserAndStatus(
                        user, OrganizerApplication.ApplicationStatus.APPROVED)
                .orElse(null);

        return convertToOrganizerProfileResponse(user, application);
    }

    @Transactional
    public OrganizerProfileResponse updateProfile(UserPrincipal currentUser, OrganizerProfileUpdateRequest request) {
        User user = getUserById(currentUser.getId());

        // Update user basic info
        user.setName(request.getName());
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            user.setEmail(request.getEmail());
        }
        user.setPhoneNumber(request.getPhone());

        User updatedUser = userRepository.save(user);

        // Update organizer application data
        OrganizerApplication application = applicationRepository.findByUserAndStatus(
                        user, OrganizerApplication.ApplicationStatus.APPROVED)
                .orElseThrow(() -> new ResourceNotFoundException("Approved organizer application not found"));

        if (request.getCompanyName() != null) {
            application.setCompanyName(request.getCompanyName());
        }
        if (request.getDescription() != null) {
            application.setDescription(request.getDescription());
        }
        if (request.getWebsite() != null) {
            application.setWebsite(request.getWebsite());
        }
        if (request.getSocialMediaLinks() != null) {
            application.setSocialMediaLinks(String.join(",", request.getSocialMediaLinks()));
        }

        OrganizerApplication updatedApplication = applicationRepository.save(application);

        return convertToOrganizerProfileResponse(updatedUser, updatedApplication);
    }

    public Page<EventSummaryResponse> getEventsByStatus(UserPrincipal currentUser, String status, Pageable pageable) {
        User user = getUserById(currentUser.getId());

        Page<Event> events;
        if ("published".equalsIgnoreCase(status)) {
            events = eventRepository.findByOrganizerIdAndPublished(user.getId(), true, pageable);
        } else if ("draft".equalsIgnoreCase(status)) {
            events = eventRepository.findByOrganizerIdAndPublished(user.getId(), false, pageable);
        } else {
            events = eventRepository.findByOrganizerId(user.getId(), pageable);
        }

        return events.map(this::convertToEventSummaryResponse);
    }

    public Page<BookingSummaryResponse> getEventBookings(UserPrincipal currentUser, Long eventId, Pageable pageable) {
        User user = getUserById(currentUser.getId());

        // Verify that the event belongs to the organizer
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.getOrganizer().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to view bookings for this event");
        }

        Page<Booking> bookings = bookingRepository.findByEvent(event, pageable);
        return bookings.map(this::convertToBookingSummaryResponse);
    }

    public AnalyticsResponse getRevenueAnalytics(UserPrincipal currentUser) {
        User user = getUserById(currentUser.getId());

        BigDecimal totalRevenue = bookingRepository.sumTotalAmountByEventOrganizerId(user.getId());
        Long confirmedBookings = bookingRepository.countByEventOrganizerIdAndStatus(
                user.getId(), Booking.BookingStatus.CONFIRMED);

        return AnalyticsResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .confirmedBookings(confirmedBookings != null ? confirmedBookings.intValue() : 0)
                .period("all-time")
                .build();
    }

    public AnalyticsResponse getBookingAnalytics(UserPrincipal currentUser) {
        User user = getUserById(currentUser.getId());

        Long totalBookings = bookingRepository.countByEventOrganizerId(user.getId());
        Long confirmedBookings = bookingRepository.countByEventOrganizerIdAndStatus(
                user.getId(), Booking.BookingStatus.CONFIRMED);
        Long pendingBookings = bookingRepository.countByEventOrganizerIdAndStatus(
                user.getId(), Booking.BookingStatus.PENDING);
        Long cancelledBookings = bookingRepository.countByEventOrganizerIdAndStatus(
                user.getId(), Booking.BookingStatus.CANCELLED);

        return AnalyticsResponse.builder()
                .totalBookings(totalBookings != null ? totalBookings.intValue() : 0)
                .confirmedBookings(confirmedBookings != null ? confirmedBookings.intValue() : 0)
                .pendingBookings(pendingBookings != null ? pendingBookings.intValue() : 0)
                .cancelledBookings(cancelledBookings != null ? cancelledBookings.intValue() : 0)
                .period("all-time")
                .build();
    }

    @Transactional
    public OrganizerProfileResponse updateOrganizerProfile(UserPrincipal currentUser, OrganizerProfileUpdateRequest request) {
        return updateProfile(currentUser, request);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private EventSummaryResponse convertToEventSummaryResponse(Event event) {
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

    private BookingSummaryResponse convertToBookingSummaryResponse(Booking booking) {
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

    private OrganizerProfileResponse convertToOrganizerProfileResponse(User user, OrganizerApplication application) {
        List<String> socialMediaLinks = null;
        if (application != null && application.getSocialMediaLinks() != null) {
            socialMediaLinks = Arrays.asList(application.getSocialMediaLinks().split(","));
        }

        return OrganizerProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhoneNumber())
                .companyName(application != null ? application.getCompanyName() : "")
                .description(application != null ? application.getDescription() : "")
                .website(application != null ? application.getWebsite() : "")
                .socialMediaLinks(socialMediaLinks)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private OrganizerApplicationResponse convertToApplicationResponse(OrganizerApplication application) {
        return OrganizerApplicationResponse.builder()
                .id(application.getId())
                .userId(application.getUser().getId())
                .userName(application.getUser().getName())
                .userEmail(application.getUser().getEmail())
                .description(application.getDescription())
                .companyName(application.getCompanyName())
                .website(application.getWebsite())
                .socialMediaLinks(application.getSocialMediaLinks())
                .status(application.getStatus().name())
                .adminFeedback(application.getAdminFeedback())
                .createdAt(application.getCreatedAt())
                .build();
    }
}

