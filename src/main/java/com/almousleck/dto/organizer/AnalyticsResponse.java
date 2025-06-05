package com.almousleck.dto.organizer;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
public class AnalyticsResponse {
    private BigDecimal totalRevenue;
    private Integer totalBookings;
    private Integer totalEvents;
    private Integer confirmedBookings;
    private Integer pendingBookings;
    private Integer cancelledBookings;
    private String period;
    private Map<String, Object> periodData;
    private Map<String, BigDecimal> revenueByMonth;
    private Map<String, Integer> bookingsByMonth;
}
