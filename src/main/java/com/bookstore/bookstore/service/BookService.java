package com.bookstore.bookstore.service;

import com.bookstore.bookstore.domain.Book;
import com.bookstore.bookstore.domain.Category;
import com.bookstore.bookstore.dto.request.BookRequest;
import com.bookstore.bookstore.dto.response.BookResponse;
import com.bookstore.bookstore.dto.response.PageResponse;
import com.bookstore.bookstore.exception.ResourceNotFoundException;
import com.bookstore.bookstore.repository.BookRepository;
import com.bookstore.bookstore.repository.CategoryRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class BookService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "author", "price");

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
    }

    @Cacheable(value = "books",
               key = "'search:' + #query + ':' + #categoryId + ':' + #minPrice + ':' + #maxPrice + ':' + #page + ':' + #size + ':' + #sortBy + ':' + #sortDir")
    public PageResponse<BookResponse> search(String query, Long categoryId,
                                             BigDecimal minPrice, BigDecimal maxPrice,
                                             int page, int size,
                                             String sortBy, String sortDir) {
        String resolvedSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "title";
        String resolvedSortDir = "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";

        List<BookResponse> content = bookRepository
                .search(query, categoryId, minPrice, maxPrice, page, size, resolvedSortBy, resolvedSortDir)
                .stream()
                .map(BookResponse::from)
                .toList();

        long total = bookRepository.countSearch(query, categoryId, minPrice, maxPrice);
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    @Cacheable(value = "books", key = "'id:' + #id")
    public BookResponse getById(Long id) {
        Book book = bookRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
        return BookResponse.from(book);
    }

    @CacheEvict(value = "books", allEntries = true)
    @Transactional
    public BookResponse create(BookRequest request) {
        Category category = resolveCategory(request.getCategoryId());
        Book book = new Book(
                request.getTitle(),
                request.getAuthor(),
                request.getDescription(),
                request.getPrice(),
                request.getStock(),
                request.getCoverImageUrl(),
                category
        );
        bookRepository.save(book);
        return BookResponse.from(book);
    }

    @CacheEvict(value = "books", allEntries = true)
    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = bookRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
        Category category = resolveCategory(request.getCategoryId());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setDescription(request.getDescription());
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        book.setCoverImageUrl(request.getCoverImageUrl());
        book.setCategory(category);
        bookRepository.save(book);
        return BookResponse.from(book);
    }

    @CacheEvict(value = "books", allEntries = true)
    @Transactional
    public void delete(Long id) {
        Book book = bookRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
        book.setDeleted(true);
        bookRepository.save(book);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }
}
