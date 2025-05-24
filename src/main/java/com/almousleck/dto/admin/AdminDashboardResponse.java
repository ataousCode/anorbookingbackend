package com.almousleck.dto.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AdminDashboardResponse {
    private long totalUsers;
    private long totalEvents;
    private long totalBookings;
    private List<EventSummary> upcomingEvents;

    @Data
    @Builder
    public static class EventSummary {
        private Long id;
        private String title;
        private LocalDateTime startDate;
        private String organizerName;
    }
}
