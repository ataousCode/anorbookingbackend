package com.almousleck.dto.event;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateEventRequest {

    @NotBlank(message = "Title cannot be blank")
    private String title;

    private String description;

    @NotBlank(message = "Location cannot be blank")
    private String location;

    @NotNull(message = "Start date cannot be null")
    @Future(message = "Start date must be in the future")
    private LocalDateTime startDate;

    @NotNull(message = "End date cannot be null")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    @NotNull(message = "Base price cannot be null")
    @Positive(message = "Base price must be positive")
    private BigDecimal basePrice;

    private String imageUrl;

    private boolean published = true;

    @NotNull(message = "Category ID cannot be null")
    private Long categoryId;

    @Valid
    private List<CreateTicketRequest> tickets;
}

