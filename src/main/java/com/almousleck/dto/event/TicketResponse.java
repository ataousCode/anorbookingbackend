package com.almousleck.dto.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TicketResponse {
    private Long id;
    private String type;
    private BigDecimal price;
    private Integer totalQuantity;
    private Integer availableQuantity;
}