package com.almousleck.service;

import com.almousleck.audit.AuditLog;
import com.almousleck.dto.admin.AdminDashboardResponse;
import com.almousleck.dto.admin.UserResponse;
import com.almousleck.dto.admin.UserStatusRequest;
import com.almousleck.dto.audit.AuditLogResponse;
import com.almousleck.dto.organizer.OrganizerApplicationResponse;
import com.almousleck.dto.organizer.OrganizerApplicationStatusRequest;
import com.almousleck.exception.BadRequestException;
import com.almousleck.exception.ResourceNotFoundException;
import com.almousleck.model.*;
import com.almousleck.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;
    private final OrganizerApplicationRepository applicationRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminDashboardResponse getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalEvents = eventRepository.count();
        long totalBookings = bookingRepository.count();

        // Add organizer-related statistics
        long pendingApplications = applicationRepository.countByStatus(OrganizerApplication.ApplicationStatus.PENDING);
        long totalOrganizers = userRepository.countByRolesName(Role.RoleName.ROLE_ORGANIZER);

        // Get upcoming events - THIS WAS MISSING
        List<Event> upcomingEvents = eventRepository.findUpcomingEvents(LocalDateTime.now(), PageRequest.of(0, 10)).getContent();

        // Get recent activities from audit logs
        List<AuditLogResponse> recentActivities = getRecentActivities();

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalEvents(totalEvents)
                .totalBookings(totalBookings)
                .pendingApplications(pendingApplications)
                .totalOrganizers(totalOrganizers)
                .upcomingEvents(upcomingEvents.stream()
                        .map(event -> AdminDashboardResponse.EventSummary.builder()
                                .id(event.getId())
                                .title(event.getTitle())
                                .startDate(event.getStartDate())
                                .organizerName(event.getOrganizer().getName())
                                .build())
                        .collect(Collectors.toList()))
                .recentActivities(recentActivities)
                .build();
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::convertToUserResponse);
    }

    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return convertToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUserStatus(Long userId, UserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setEnabled(request.isEnabled());
        User updatedUser = userRepository.save(user);

        return convertToUserResponse(updatedUser);
    }

    // NEW: Organizer Application Management Methods
    public Page<OrganizerApplicationResponse> getOrganizerApplications(Pageable pageable, String status) {
        Page<OrganizerApplication> applications;

        if (status != null && !status.isEmpty()) {
            try {
                OrganizerApplication.ApplicationStatus applicationStatus =
                        OrganizerApplication.ApplicationStatus.valueOf(status.toUpperCase());
                applications = applicationRepository.findByStatus(applicationStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status: " + status);
            }
        } else {
            applications = applicationRepository.findAll(pageable);
        }

        return applications.map(this::convertToApplicationResponse);
    }

    @Transactional
    public OrganizerApplicationResponse updateOrganizerApplicationStatus(
            Long applicationId,
            OrganizerApplicationStatusRequest request) {

        OrganizerApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", "id", applicationId));

        // Validate status
        OrganizerApplication.ApplicationStatus newStatus;
        try {
            newStatus = OrganizerApplication.ApplicationStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + request.getStatus());
        }

        application.setStatus(newStatus);
        application.setAdminFeedback(request.getAdminFeedback());

        if (newStatus == OrganizerApplication.ApplicationStatus.APPROVED) {
            User user = application.getUser();
            Role organizerRole = roleRepository.findByName(Role.RoleName.ROLE_ORGANIZER)
                    .orElseThrow(() -> new RuntimeException("Organizer role not found"));

            if (!user.getRoles().contains(organizerRole)) {
                user.getRoles().add(organizerRole);
                userRepository.save(user);
            }
        }

        OrganizerApplication savedApplication = applicationRepository.save(application);
        return convertToApplicationResponse(savedApplication);
    }

    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
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

    private List<AuditLogResponse> getRecentActivities() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> auditLogs = auditLogRepository.findAll(pageable);
        return auditLogs.getContent().stream()
                .map(this::convertToAuditLogResponse)
                .collect(Collectors.toList());
    }

    private AuditLogResponse convertToAuditLogResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .userId(auditLog.getUserId())
                .username(auditLog.getUsername())
                .details(auditLog.getDetails())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}