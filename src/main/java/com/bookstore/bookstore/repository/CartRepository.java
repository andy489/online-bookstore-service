package com.bookstore.bookstore.repository;

import com.bookstore.bookstore.domain.Cart;

import java.util.Optional;

public interface CartRepository {
    Optional<Cart> findByUserId(Long userId);
    Cart save(Cart cart);
}
