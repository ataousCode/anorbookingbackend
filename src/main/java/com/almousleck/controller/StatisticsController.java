package com.almousleck.controller;

import com.almousleck.dto.statistics.EventStatisticsResponse;
import com.almousleck.dto.statistics.OrganizerStatisticsResponse;
import com.almousleck.security.CurrentUser;
import com.almousleck.security.UserPrincipal;
import com.almousleck.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/events/{eventId}")
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<EventStatisticsResponse> getEventStatistics(
            @CurrentUser UserPrincipal currentUser,
            @PathVariable Long eventId) {
        return ResponseEntity.ok(statisticsService.getEventStatistics(currentUser, eventId));
    }

    @GetMapping("/organizer")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<OrganizerStatisticsResponse> getOrganizerStatistics(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(statisticsService.getOrganizerStatistics(currentUser));
    }
}

