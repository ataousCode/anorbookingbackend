package com.almousleck.dto.statistics;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class OrganizerStatisticsResponse {
    private Long organizerId;
    private String organizerName;
    private Long totalEvents;
    private Long publishedEvents;
    private Long upcomingEvents;
    private Long totalBookings;
    private BigDecimal totalRevenue;
    private List<EventStatisticsResponse> topEvents;
}
