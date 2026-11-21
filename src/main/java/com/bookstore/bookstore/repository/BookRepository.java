package com.bookstore.bookstore.repository;

import com.bookstore.bookstore.domain.Book;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BookRepository {
    Optional<Book> findById(Long id);
    Optional<Book> findActiveById(Long id);
    Book save(Book book);
    void delete(Book book);

    List<Book> search(String query, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                      int page, int size, String sortBy, String sortDir);

    long countSearch(String query, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice);

    long countByCategory(com.bookstore.bookstore.domain.Category category);
}
