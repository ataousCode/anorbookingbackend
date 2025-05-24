package com.almousleck.controller;

import com.almousleck.dto.search.SearchRequest;
import com.almousleck.dto.search.SearchResponse;
import com.almousleck.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/events")
    public ResponseEntity<SearchResponse> searchEvents(@RequestBody SearchRequest request) {
        return ResponseEntity.ok(searchService.searchEvents(request));
    }
}

