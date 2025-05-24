package com.almousleck.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class BookingDetailResponse {
    private Long id;
    private String bookingReference;
    private Long eventId;
    private String eventTitle;
    private String eventLocation;
    private LocalDateTime eventDate;
    private Long ticketId;
    private String ticketType;
    private Integer quantity;
    private BigDecimal totalAmount;
    private String status;
    private String userName;
    private String userEmail;
    private LocalDateTime createdAt;
}

