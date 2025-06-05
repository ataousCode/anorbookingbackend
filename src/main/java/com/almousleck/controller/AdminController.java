package com.almousleck.controller;

import com.almousleck.dto.admin.AdminDashboardResponse;
import com.almousleck.dto.admin.UserResponse;
import com.almousleck.dto.admin.UserStatusRequest;
import com.almousleck.dto.organizer.OrganizerApplicationResponse;
import com.almousleck.dto.organizer.OrganizerApplicationStatusRequest;
import com.almousleck.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboardStats() {
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUserById(userId));
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusRequest request) {
        return ResponseEntity.ok(adminService.updateUserStatus(userId, request));
    }

    @GetMapping("/organizer-applications")
    public ResponseEntity<Page<OrganizerApplicationResponse>> getOrganizerApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.getOrganizerApplications(pageable, status));
    }

    @PutMapping("/organizer-applications/{applicationId}/status")
    public ResponseEntity<OrganizerApplicationResponse> updateOrganizerApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody OrganizerApplicationStatusRequest request) {
        return ResponseEntity.ok(adminService.updateOrganizerApplicationStatus(applicationId, request));
    }
}

