package com.almousleck.dto.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class EventDetailResponse {
    private Long id;
    private String title;
    private String description;
    private String location;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal basePrice;
    private String imageUrl;
    private boolean published;
    private Long categoryId;
    private String categoryName;
    private Long organizerId;
    private String organizerName;
    private List<TicketResponse> tickets;
}
