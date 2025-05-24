package com.almousleck.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingSummaryResponse {
    private Long id;
    private String bookingReference;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String ticketType;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
}
