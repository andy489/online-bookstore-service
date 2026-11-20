package com.bookstore.bookstore.dto.response;

import com.bookstore.bookstore.domain.Book;

import java.math.BigDecimal;
import java.time.Instant;

public record BookResponse(
        Long id,
        String title,
        String author,
        String description,
        BigDecimal price,
        Integer stock,
        String coverImageUrl,
        Long categoryId,
        String categoryName,
        Instant createdAt,
        Instant updatedAt
) {
    public static BookResponse from(Book book) {
        Long catId = book.getCategory() != null ? book.getCategory().getId() : null;
        String catName = book.getCategory() != null ? book.getCategory().getName() : null;
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getDescription(),
                book.getPrice(),
                book.getStock(),
                book.getCoverImageUrl(),
                catId,
                catName,
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }
}
