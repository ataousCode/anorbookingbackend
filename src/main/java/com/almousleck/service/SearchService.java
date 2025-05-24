package com.almousleck.service;

import com.almousleck.dto.search.SearchRequest;
import com.almousleck.dto.search.SearchResponse;
import com.almousleck.model.Event;
import com.almousleck.model.EventCategory;
import com.almousleck.repository.EventCategoryRepository;
import com.almousleck.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;

    public SearchResponse searchEvents(SearchRequest request) {
        // Build sort
        Sort sort = buildSort(request.getSortBy(), request.getSortDirection());

        // Build pageable
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                sort);

        // Apply filters
        Page<Event> eventsPage;

        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            eventsPage = eventRepository.searchEvents(request.getKeyword(), pageable);
        } else if (request.getCategoryId() != null) {
            EventCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElse(null);

            if (category != null) {
                if (request.getUpcomingOnly()) {
                    eventsPage = eventRepository.findUpcomingEventsByCategory(category, LocalDateTime.now(), pageable);
                } else {
                    eventsPage = eventRepository.findByPublishedTrueAndCategory(category, pageable);
                }
            } else {
                eventsPage = eventRepository.findByPublishedTrue(pageable);
            }
        } else if (request.getUpcomingOnly()) {
            eventsPage = eventRepository.findUpcomingEvents(LocalDateTime.now(), pageable);
        } else {
            eventsPage = eventRepository.findByPublishedTrue(pageable);
        }

        // Convert to response
        List<SearchResponse.EventResult> events = eventsPage.getContent().stream()
                .map(event -> SearchResponse.EventResult.builder()
                        .id(event.getId())
                        .title(event.getTitle())
                        .description(event.getDescription())
                        .location(event.getLocation())
                        .startDate(event.getStartDate())
                        .endDate(event.getEndDate())
                        .basePrice(event.getBasePrice())
                        .imageUrl(event.getImageUrl())
                        .categoryId(event.getCategory().getId())
                        .categoryName(event.getCategory().getName())
                        .organizerId(event.getOrganizer().getId())
                        .organizerName(event.getOrganizer().getName())
                        .build())
                .collect(Collectors.toList());

        // Get categories for facets
        List<SearchResponse.CategoryFacet> categories = new ArrayList<>();
        if (request.getIncludeFacets()) {
            categories = categoryRepository.findByActiveTrue().stream()
                    .map(category -> SearchResponse.CategoryFacet.builder()
                            .id(category.getId())
                            .name(category.getName())
                            .count(eventRepository.countByCategoryAndPublishedTrue(category))
                            .build())
                    .collect(Collectors.toList());
        }

        return SearchResponse.builder()
                .events(events)
                .totalElements(eventsPage.getTotalElements())
                .totalPages(eventsPage.getTotalPages())
                .currentPage(eventsPage.getNumber())
                .size(eventsPage.getSize())
                .categories(categories)
                .build();
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        Sort.Direction direction = Sort.Direction.ASC;
        if ("desc".equalsIgnoreCase(sortDirection)) {
            direction = Sort.Direction.DESC;
        }

        String sortField = "startDate";
        if (sortBy != null) {
            switch (sortBy.toLowerCase()) {
                case "title":
                    sortField = "title";
                    break;
                case "price":
                    sortField = "basePrice";
                    break;
                case "created":
                    sortField = "createdAt";
                    break;
                default:
                    sortField = "startDate";
            }
        }

        return Sort.by(direction, sortField);
    }
}
