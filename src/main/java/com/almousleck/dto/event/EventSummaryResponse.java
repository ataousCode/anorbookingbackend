package com.almousleck.dto.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class EventSummaryResponse {
    private Long id;
    private String title;
    private String location;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal basePrice;
    private String imageUrl;
    private Long categoryId;
    private String categoryName;
    private Long organizerId;
    private String organizerName;
    private String status;
}
