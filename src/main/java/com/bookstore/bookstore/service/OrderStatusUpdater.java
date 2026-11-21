package com.bookstore.bookstore.service;

import com.bookstore.bookstore.domain.*;
import com.bookstore.bookstore.dto.request.UpdateOrderStatusRequest;
import com.bookstore.bookstore.dto.response.OrderResponse;
import com.bookstore.bookstore.exception.InvalidOperationException;
import com.bookstore.bookstore.exception.ResourceNotFoundException;
import com.bookstore.bookstore.repository.BookRepository;
import com.bookstore.bookstore.repository.OrderRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Extracted so that @Transactional is applied by the Spring proxy on a public method
 * of a separate bean — self-invocation on OrderService would silently bypass the proxy.
 */
@Component
public class OrderStatusUpdater {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS;
    static {
        Map<OrderStatus, Set<OrderStatus>> m = new EnumMap<>(OrderStatus.class);
        m.put(OrderStatus.PENDING,   EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        m.put(OrderStatus.SHIPPED,   EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED));
        m.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        m.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS = m;
    }

    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;

    public OrderStatusUpdater(OrderRepository orderRepository, BookRepository bookRepository) {
        this.orderRepository = orderRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public OrderResponse update(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        OrderStatus current = order.getStatus();
        OrderStatus next = request.getStatus();
        if (!ALLOWED_TRANSITIONS.get(current).contains(next)) {
            throw new InvalidOperationException(
                    "Cannot transition order from " + current + " to " + next);
        }

        if (next == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Book book = item.getBook();
                book.setStock(book.getStock() + item.getQuantity());
                bookRepository.save(book);
            }
        }

        order.setStatus(next);
        orderRepository.save(order);
        return OrderResponse.from(order);
    }
}
