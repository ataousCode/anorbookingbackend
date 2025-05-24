package com.almousleck.service;

import com.almousleck.dto.category.CategoryRequest;
import com.almousleck.dto.category.CategoryResponse;
import com.almousleck.exception.BadRequestException;
import com.almousleck.exception.ResourceNotFoundException;
import com.almousleck.model.EventCategory;
import com.almousleck.repository.EventCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final EventCategoryRepository categoryRepository;

    public List<CategoryResponse> getAllCategories() {
        List<EventCategory> categories = categoryRepository.findAll();
        return categories.stream()
                .map(this::convertToCategoryResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> getActiveCategories() {
        List<EventCategory> categories = categoryRepository.findByActiveTrue();
        return categories.stream()
                .map(this::convertToCategoryResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getCategoryById(Long categoryId) {
        EventCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("EventCategory", "id", categoryId));

        return convertToCategoryResponse(category);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        if (categoryRepository.existsByName(categoryRequest.getName())) {
            throw new BadRequestException("Category name already exists");
        }

        EventCategory category = EventCategory.builder()
                .name(categoryRequest.getName())
                .description(categoryRequest.getDescription())
                .iconUrl(categoryRequest.getIconUrl())
                .active(categoryRequest.isActive())
                .build();

        EventCategory savedCategory = categoryRepository.save(category);

        return convertToCategoryResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryRequest categoryRequest) {
        EventCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("EventCategory", "id", categoryId));

        if (!category.getName().equals(categoryRequest.getName()) &&
                categoryRepository.existsByName(categoryRequest.getName())) {
            throw new BadRequestException("Category name already exists");
        }

        category.setName(categoryRequest.getName());
        category.setDescription(categoryRequest.getDescription());
        category.setIconUrl(categoryRequest.getIconUrl());
        category.setActive(categoryRequest.isActive());

        EventCategory updatedCategory = categoryRepository.save(category);

        return convertToCategoryResponse(updatedCategory);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        EventCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("EventCategory", "id", categoryId));

        // Check if there are any events in this category
        // This would require an EventRepository and additional logic

        categoryRepository.delete(category);
    }

    private CategoryResponse convertToCategoryResponse(EventCategory category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .active(category.isActive())
                .build();
    }
}
