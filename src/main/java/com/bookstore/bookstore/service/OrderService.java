package com.bookstore.bookstore.service;

import com.bookstore.bookstore.domain.*;
import com.bookstore.bookstore.dto.request.UpdateOrderStatusRequest;
import com.bookstore.bookstore.dto.response.OrderAdjustment;
import com.bookstore.bookstore.dto.response.OrderResponse;
import com.bookstore.bookstore.dto.response.PageResponse;
import com.bookstore.bookstore.exception.InvalidOperationException;
import com.bookstore.bookstore.exception.ResourceNotFoundException;
import com.bookstore.bookstore.repository.CartRepository;
import com.bookstore.bookstore.repository.OrderRepository;
import com.bookstore.bookstore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public OrderResponse placeOrder(String email) {
        User user = resolveUser(email);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new InvalidOperationException("Cannot place order with an empty cart"));

        if (cart.getItems().isEmpty()) {
            throw new InvalidOperationException("Cannot place order with an empty cart");
        }

        Order order = new Order(user, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;
        List<OrderAdjustment> adjustments = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            Book book = item.getBook();
            int requested = item.getQuantity();
            int available = book.getStock();

            if (available == 0) {
                adjustments.add(new OrderAdjustment(book.getTitle(), requested, 0, "Out of stock"));
                continue;
            }

            int fulfilled = Math.min(requested, available);
            if (fulfilled < requested) {
                adjustments.add(new OrderAdjustment(book.getTitle(), requested, fulfilled,
                        "Only " + available + " in stock"));
            }

            book.setStock(available - fulfilled);

            order.getItems().add(new OrderItem(order, book, fulfilled, book.getPrice()));
            total = total.add(book.getPrice().multiply(BigDecimal.valueOf(fulfilled)));
        }

        if (order.getItems().isEmpty()) {
            throw new InvalidOperationException("None of the items in your cart are in stock");
        }

        order.setTotalPrice(total);
        orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return OrderResponse.from(order, adjustments);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String email) {
        User user = resolveUser(email);
        return orderRepository.findByUserId(user.getId()).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getById(Long orderId, String email, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        User user = resolveUser(email);
        if (!isAdmin && !order.getUser().getId().equals(user.getId())) {
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

    @Transactional
    public OrderResponse updateStatus(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOperationException("Cannot transition order out of CANCELLED status");
        }
        order.setStatus(request.getStatus());
        orderRepository.save(order);
        return OrderResponse.from(order);
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
