package com.bookstore.bookstore.dto.response;

import com.bookstore.bookstore.domain.Category;

public record CategoryResponse(Long id, String name, String description) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
