package com.almousleck.dto.statistics;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EventStatisticsResponse {
    private Long eventId;
    private String eventTitle;
    private Long totalBookings;
    private Integer totalTicketsAvailable;
    private Integer totalTicketsSold;
    private BigDecimal totalRevenue;
}
