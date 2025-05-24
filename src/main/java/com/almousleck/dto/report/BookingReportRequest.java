package com.almousleck.dto.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BookingReportRequest {
    private Long eventId;
    private Boolean userBookingsOnly = false;
    private Boolean organizerBookingsOnly = false;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
}
