package com.bookstore.bookstore.integration;

import com.bookstore.bookstore.dto.response.BookResponse;
import com.bookstore.bookstore.dto.response.PageResponse;
import com.bookstore.bookstore.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies BookService.search() against a real PostgreSQL instance with Flyway migrations applied.
 * Ensures Criteria API dynamic queries produce correct results — something unit tests with mocks cannot catch.
 */
class BookRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    BookService bookService;

    @Test
    void search_noFilters_returnsAllBooks() {
        var result = bookService.search(null, null, null, null, 0, 100, "title", "asc");
        assertThat(result.getContent()).isNotEmpty();
    }

    @Test
    void search_byQuery_returnsMatchingBooks() {
        var result = bookService.search("Dune", null, null, null, 0, 10, "title", "asc");
        assertThat(result.getContent())
                .isNotEmpty()
                .allMatch(b -> b.getTitle().contains("Dune") || b.getAuthor().contains("Herbert"));
    }

    @Test
    void search_byPriceRange_returnsOnlyBooksInRange() {
        BigDecimal min = new BigDecimal("10.00");
        BigDecimal max = new BigDecimal("12.00");
        var result = bookService.search(null, null, min, max, 0, 100, "price", "asc");
        assertThat(result.getContent()).isNotEmpty();
        result.getContent().forEach(b ->
                assertThat(b.getPrice())
                        .isGreaterThanOrEqualTo(min)
                        .isLessThanOrEqualTo(max)
        );
    }

    @Test
    void search_byCategoryThatDoesNotExist_returnsEmpty() {
        var result = bookService.search(null, Long.MAX_VALUE, null, null, 0, 10, "title", "asc");
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void search_pagination_secondPageHasFewerItems() {
        var page0 = bookService.search(null, null, null, null, 0, 5, "title", "asc");
        var page1 = bookService.search(null, null, null, null, 1, 5, "title", "asc");
        assertThat(page0.getContent()).hasSize(5);
        assertThat(page0.getContent()).doesNotContainAnyElementsOf(
                page1.getContent().stream()
                        .map(BookResponse::getTitle)
                        .map(title -> page0.getContent().stream()
                                .filter(b -> b.getTitle().equals(title))
                                .findFirst().orElse(null))
                        .toList()
        );
    }
}
