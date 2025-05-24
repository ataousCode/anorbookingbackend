package com.almousleck.dto.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventReportRequest {
    private Boolean organizerOnly = true;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long categoryId;
}
