package com.bookstore.bookstore.repository;

import com.bookstore.bookstore.domain.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Optional<Order> findById(Long id);
    List<Order> findByUserId(Long userId, int page, int size);
    long countByUserId(Long userId);
    List<Order> findAll(int page, int size);
    long countAll();
    Order save(Order order);
}
