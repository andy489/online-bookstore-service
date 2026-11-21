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
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
public class BookService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "author", "price");

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;

    public BookService(BookRepository bookRepository, CategoryRepository categoryRepository,
                       CloudinaryService cloudinaryService) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Cacheable(value = "books",
               key = "'search:' + T(java.util.Objects).toString(#query,'') + ':' + #categoryId + ':' + T(com.bookstore.bookstore.service.BookService).normalizeBd(#minPrice) + ':' + T(com.bookstore.bookstore.service.BookService).normalizeBd(#maxPrice) + ':' + #page + ':' + #size + ':' + T(com.bookstore.bookstore.service.BookService).resolvedSort(#sortBy) + ':' + T(com.bookstore.bookstore.service.BookService).resolvedDir(#sortDir)")
    @Transactional(readOnly = true)
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

        // Skip the count query when the first page is not full — total is trivially content.size()
        long total = (page == 0 && content.size() < size)
                ? content.size()
                : bookRepository.countSearch(query, categoryId, minPrice, maxPrice);
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    @Cacheable(value = "books", key = "'id:' + #id")
    @Transactional(readOnly = true)
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

        String oldCoverUrl = book.getCoverImageUrl();
        String newCoverUrl = request.getCoverImageUrl();
        if (oldCoverUrl != null && !oldCoverUrl.equals(newCoverUrl)
                && oldCoverUrl.contains("cloudinary.com")) {
            // Best-effort delete; don't fail the update if Cloudinary is unreachable
            try {
                cloudinaryService.deleteCover("book-" + id);
            } catch (Exception ignored) {
            }
        }

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setDescription(request.getDescription());
        book.setPrice(request.getPrice());
        book.setStock(request.getStock());
        book.setCoverImageUrl(newCoverUrl);
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

    @CacheEvict(value = "books", allEntries = true)
    @Transactional
    public BookResponse uploadCover(Long id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.bookstore.bookstore.exception.InvalidOperationException("Cover image file must not be empty");
        }
        Book book = bookRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + id));
        String publicId = "book-" + id;
        String url = cloudinaryService.uploadCover(file, publicId);
        book.setCoverImageUrl(url);
        bookRepository.save(book);
        return BookResponse.from(book);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }

    // These helpers must remain public: @Cacheable SpEL T(...) expressions call them by reflection.
    public static String normalizeBd(java.math.BigDecimal d) {
        return d == null ? "" : d.stripTrailingZeros().toPlainString();
    }

    // See normalizeBd — same SpEL T(...) constraint.
    public static String resolvedSort(String sortBy) {
        return ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "title";
    }

    // See normalizeBd — same SpEL T(...) constraint.
    public static String resolvedDir(String sortDir) {
        return "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc";
    }
}
