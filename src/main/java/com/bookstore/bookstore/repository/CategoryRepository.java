package com.bookstore.bookstore.repository;

import com.bookstore.bookstore.domain.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Optional<Category> findById(Long id);
    Optional<Category> findByName(String name);
    List<Category> findAll();
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    Category save(Category category);
    void delete(Category category);
}
