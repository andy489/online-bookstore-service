package com.bookstore.bookstore.service;

import com.bookstore.bookstore.domain.*;
import com.bookstore.bookstore.dto.request.UpdateOrderStatusRequest;
import com.bookstore.bookstore.dto.response.OrderResponse;
import com.bookstore.bookstore.exception.InsufficientStockException;
import com.bookstore.bookstore.exception.InvalidOperationException;
import com.bookstore.bookstore.exception.ResourceNotFoundException;
import com.bookstore.bookstore.repository.BookRepository;
import com.bookstore.bookstore.repository.CartRepository;
import com.bookstore.bookstore.repository.OrderRepository;
import com.bookstore.bookstore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository,
                        BookRepository bookRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
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

        for (CartItem item : cart.getItems()) {
            if (item.getBook().getStock() < item.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for '" + item.getBook().getTitle() + "': requested " +
                        item.getQuantity() + ", available " + item.getBook().getStock());
            }
        }

        Order order = new Order(user, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            Book book = item.getBook();
            book.setStock(book.getStock() - item.getQuantity());
            bookRepository.save(book);

            order.getItems().add(new OrderItem(order, book, item.getQuantity(), book.getPrice()));
            total = total.add(book.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        order.setTotalPrice(total);
        orderRepository.save(order);

        cart.getItems().clear();
        cartRepository.save(cart);

        return OrderResponse.from(order);
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
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
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
