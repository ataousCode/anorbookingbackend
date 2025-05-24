package com.almousleck.controller;

import com.almousleck.dto.organizer.OrganizerApplicationRequest;
import com.almousleck.dto.organizer.OrganizerApplicationResponse;
import com.almousleck.dto.organizer.OrganizerApplicationStatusRequest;
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

@RestController
@RequestMapping("/organizers")
@RequiredArgsConstructor
public class OrganizerController {

    private final OrganizerService organizerService;

    @PostMapping("/apply")
    public ResponseEntity<OrganizerApplicationResponse> applyForOrganizerRole(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody OrganizerApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(organizerService.applyForOrganizerRole(currentUser, request));
    }

    @GetMapping("/application-status")
    public ResponseEntity<OrganizerApplicationResponse> getApplicationStatus(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(organizerService.getApplicationStatus(currentUser));
    }

    @GetMapping("/pending-applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrganizerApplicationResponse>> getPendingApplications(Pageable pageable) {
        return ResponseEntity.ok(organizerService.getPendingApplications(pageable));
    }

    @PutMapping("/applications/{applicationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrganizerApplicationResponse> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody OrganizerApplicationStatusRequest request) {
        return ResponseEntity.ok(organizerService.updateApplicationStatus(applicationId, request));
    }
}

