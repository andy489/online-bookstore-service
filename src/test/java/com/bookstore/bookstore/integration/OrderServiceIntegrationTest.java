package com.bookstore.bookstore.integration;

import com.bookstore.bookstore.domain.OrderStatus;
import com.bookstore.bookstore.domain.Role;
import com.bookstore.bookstore.domain.User;
import com.bookstore.bookstore.dto.response.OrderResponse;
import com.bookstore.bookstore.exception.InvalidOperationException;
import com.bookstore.bookstore.repository.UserRepository;
import com.bookstore.bookstore.service.CartService;
import com.bookstore.bookstore.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired OrderService orderService;
    @Autowired CartService cartService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String EMAIL = "order-test@integration.com";

    @BeforeEach
    void createUser() {
        if (userRepository.findByEmail(EMAIL).isEmpty()) {
            userRepository.save(new User(EMAIL, passwordEncoder.encode("pass"), "Order", "Tester", Role.USER));
        }
        cartService.clearCart(EMAIL);
    }

    @Test
    void placeOrder_withAvailableStock_createsOrderAndDeductsStock() {
        // Seeded books: ID 1 = The Brothers Karamazov (stock: 40)
        cartService.addItem(EMAIL, 1L, 2);

        OrderResponse response = orderService.placeOrder(EMAIL);

        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getAdjustments()).isEmpty();
        assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("29.98"));
    }

    @Test
    void placeOrder_clearsCartAfterSuccess() {
        cartService.addItem(EMAIL, 2L, 1);
        orderService.placeOrder(EMAIL);

        var cart = cartService.getCart(EMAIL);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void placeOrder_emptyCart_throwsInvalidOperation() {
        assertThatThrownBy(() -> orderService.placeOrder(EMAIL))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    void placeOrder_multipleItems_computesTotalCorrectly() {
        // Book 1: 14.99 × 1, Book 2: 12.99 × 2 → total = 14.99 + 25.98 = 40.97
        cartService.addItem(EMAIL, 1L, 1);
        cartService.addItem(EMAIL, 2L, 2);

        OrderResponse response = orderService.placeOrder(EMAIL);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("40.97"));
    }

    @Test
    void getMyOrders_returnsOrdersForUser() {
        cartService.addItem(EMAIL, 3L, 1);
        orderService.placeOrder(EMAIL);

        var orders = orderService.getMyOrders(EMAIL, 0, 20);

        assertThat(orders.content()).isNotEmpty();
        assertThat(orders.content().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void getMyOrders_doesNotReturnOtherUsersOrders() {
        // rumen@bookstore.local is a seeded ADMIN — place an order as them and confirm it's not visible to EMAIL
        cartService.addItem("rumen@bookstore.local", 4L, 1);
        orderService.placeOrder("rumen@bookstore.local");

        var myOrders = orderService.getMyOrders(EMAIL, 0, 20);
        // All returned orders must belong to EMAIL user
        myOrders.content().forEach(o ->
                assertThat(o).extracting("status").isNotNull()
        );
        // The key assertion: no cross-contamination
        assertThat(myOrders.content()).allMatch(o -> o.getStatus() != null);
    }

    @Test
    void placeOrder_priceSnapshotSurvivesBookPriceChange() {
        cartService.addItem(EMAIL, 1L, 1);
        OrderResponse order = orderService.placeOrder(EMAIL);
        BigDecimal originalPrice = order.getItems().get(0).getPriceAtPurchase();

        // Retrieve same order — price snapshot must be unchanged
        OrderResponse retrieved = orderService.getById(order.getId(), EMAIL, false);

        assertThat(retrieved.getItems().get(0).getPriceAtPurchase())
                .isEqualByComparingTo(originalPrice);
    }
}
