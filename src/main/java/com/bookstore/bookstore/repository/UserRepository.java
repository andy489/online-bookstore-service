package com.bookstore.bookstore.repository;

import com.bookstore.bookstore.domain.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    boolean existsByEmail(String email);
    User save(User user);
}
