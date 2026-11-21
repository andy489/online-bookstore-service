package com.bookstore.bookstore.integration;

import com.bookstore.bookstore.dto.response.BookResponse;
import com.bookstore.bookstore.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies Spring Cache + Redis integration: @Cacheable populates the cache on a miss
 * and @CacheEvict clears it after a mutation — tested against real Redis.
 */
class CacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    BookService bookService;

    @Autowired
    CacheManager cacheManager;

    @Test
    void getById_populatesCacheOnFirstCall() {
        // Find a valid ID from seeded data
        var allBooks = bookService.search(null, null, null, null, 0, 1, "title", "asc");
        assertThat(allBooks.content()).isNotEmpty();
        Long id = allBooks.content().get(0).id();

        // First call — cache miss, hits DB
        BookResponse first = bookService.getById(id);

        // Second call — should come from cache (same result)
        BookResponse second = bookService.getById(id);

        assertThat(first.title()).isEqualTo(second.title());

        // Cache entry should exist
        var cache = cacheManager.getCache("books");
        assertThat(cache).isNotNull();
        assertThat(cache.get("id:" + id)).isNotNull();
    }

    @Test
    void delete_evictsCache() {
        var allBooks = bookService.search(null, null, null, null, 0, 1, "title", "asc");
        Long id = allBooks.content().get(0).id();

        // Populate cache
        bookService.getById(id);

        // Soft-delete evicts all entries
        bookService.delete(id);

        var cache = cacheManager.getCache("books");
        assertThat(cache).isNotNull();
        // After eviction the entry should be gone
        assertThat(cache.get("id:" + id)).isNull();
    }
}
