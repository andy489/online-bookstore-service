package com.bookstore.bookstore.service;

import com.bookstore.bookstore.domain.*;
import com.bookstore.bookstore.dto.request.UpdateOrderStatusRequest;
import com.bookstore.bookstore.dto.response.OrderResponse;
import com.bookstore.bookstore.dto.response.PageResponse;
import com.bookstore.bookstore.exception.InvalidOperationException;
import com.bookstore.bookstore.exception.ResourceNotFoundException;
import com.bookstore.bookstore.repository.OrderRepository;
import com.bookstore.bookstore.repository.UserRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private static final int MAX_PLACE_ORDER_RETRIES = 3;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderPlacer orderPlacer;
    private final OrderStatusUpdater orderStatusUpdater;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        OrderPlacer orderPlacer, OrderStatusUpdater orderStatusUpdater) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderPlacer = orderPlacer;
        this.orderStatusUpdater = orderStatusUpdater;
    }

    public OrderResponse placeOrder(String email) {
        User user = resolveUser(email);
        for (int attempt = 1; attempt <= MAX_PLACE_ORDER_RETRIES; attempt++) {
            try {
                return orderPlacer.place(user);
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == MAX_PLACE_ORDER_RETRIES) {
                    throw new InvalidOperationException(
                            "Could not complete your order due to concurrent stock updates — please try again");
                }
            }
        }
        throw new InvalidOperationException("Order placement failed unexpectedly");
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getMyOrders(String email, int page, int size) {
        User user = resolveUser(email);
        List<OrderResponse> content = orderRepository.findByUserId(user.getId(), page, size).stream()
                .map(OrderResponse::from)
                .toList();
        long total = orderRepository.countByUserId(user.getId());
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId, String email, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (isAdmin) {
            return OrderResponse.from(order);
        }
        User user = resolveUser(email);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order not found: " + orderId);
        }
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(int page, int size) {
        List<OrderResponse> content = orderRepository.findAll(page, size).stream()
                .map(OrderResponse::from)
                .toList();
        long total = orderRepository.countAll();
        int totalPages = (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, totalPages);
    }

    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        for (int attempt = 1; attempt <= MAX_PLACE_ORDER_RETRIES; attempt++) {
            try {
                return orderStatusUpdater.update(orderId, request);
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == MAX_PLACE_ORDER_RETRIES) {
                    throw new InvalidOperationException(
                            "Could not update order status due to concurrent modifications — please retry");
                }
            }
        }
        throw new InvalidOperationException("Order status update failed unexpectedly");
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
