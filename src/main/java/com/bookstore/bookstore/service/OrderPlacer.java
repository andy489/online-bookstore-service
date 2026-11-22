package com.bookstore.bookstore.service;

import com.bookstore.bookstore.domain.*;
import com.bookstore.bookstore.dto.response.OrderAdjustment;
import com.bookstore.bookstore.dto.response.OrderResponse;
import com.bookstore.bookstore.exception.InvalidOperationException;
import com.bookstore.bookstore.repository.BookRepository;
import com.bookstore.bookstore.repository.CartRepository;
import com.bookstore.bookstore.repository.OrderRepository;
import com.bookstore.bookstore.repository.UserRepository;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Extracted so that @Transactional is applied by the Spring proxy on a public method
 * of a separate bean — self-invocation on OrderService would silently bypass the proxy.
 */
@Component
public class OrderPlacer {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public OrderPlacer(OrderRepository orderRepository, CartRepository cartRepository,
                       BookRepository bookRepository, UserRepository userRepository,
                       CacheManager cacheManager) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public OrderResponse place(User detachedUser) {
        // Bug #4 fix: re-load User inside the transaction to get a managed entity
        User user = userRepository.findById(detachedUser.getId())
                .orElseThrow(() -> new InvalidOperationException("User not found"));

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

            // Bug #2 fix: explicitly save the book after stock decrement
            book.setStock(available - fulfilled);
            bookRepository.save(book);

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

        // Stock changed — evict the books cache so inStock reflects reality
        Objects.requireNonNull(cacheManager.getCache("books")).clear();

        return OrderResponse.from(order, adjustments);
    }
}
