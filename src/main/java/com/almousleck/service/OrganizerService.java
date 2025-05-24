package com.almousleck.service;

import com.almousleck.dto.organizer.OrganizerApplicationRequest;
import com.almousleck.dto.organizer.OrganizerApplicationResponse;
import com.almousleck.dto.organizer.OrganizerApplicationStatusRequest;
import com.almousleck.exception.BadRequestException;
import com.almousleck.exception.ResourceNotFoundException;
import com.almousleck.model.OrganizerApplication;
import com.almousleck.model.Role;
import com.almousleck.model.User;
import com.almousleck.repository.OrganizerApplicationRepository;
import com.almousleck.repository.RoleRepository;
import com.almousleck.repository.UserRepository;
import com.almousleck.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrganizerService {

    private final OrganizerApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;

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

        // If approved, add organizer role to user
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

