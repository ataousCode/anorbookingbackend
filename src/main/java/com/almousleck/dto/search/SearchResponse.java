package com.almousleck.dto.search;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SearchResponse {
    private List<EventResult> events;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int size;
    private List<CategoryFacet> categories;

    @Data
    @Builder
    public static class EventResult {
        private Long id;
        private String title;
        private String description;
        private String location;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private BigDecimal basePrice;
        private String imageUrl;
        private Long categoryId;
        private String categoryName;
        private Long organizerId;
        private String organizerName;
    }

    @Data
    @Builder
    public static class CategoryFacet {
        private Long id;
        private String name;
        private Long count;
    }
}
