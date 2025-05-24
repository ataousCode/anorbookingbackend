package com.almousleck.dto.search;

import lombok.Data;

@Data
public class SearchRequest {
    private String keyword;
    private Long categoryId;
    private Boolean upcomingOnly = true;
    private Boolean includeFacets = true;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "startDate";
    private String sortDirection = "asc";
}
