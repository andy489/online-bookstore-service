package com.bookstore.bookstore.unit;

import com.bookstore.bookstore.domain.*;
import com.bookstore.bookstore.dto.response.OrderResponse;
import com.bookstore.bookstore.exception.InvalidOperationException;
import com.bookstore.bookstore.repository.CartRepository;
import com.bookstore.bookstore.repository.OrderRepository;
import com.bookstore.bookstore.repository.UserRepository;
import com.bookstore.bookstore.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceUnitTest {

    @Mock OrderRepository orderRepository;
    @Mock CartRepository cartRepository;
    @Mock UserRepository userRepository;

    @InjectMocks OrderService orderService;

    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        user = new User("user@test.com", "hash", "Test", "User", Role.USER);
        category = new Category("Fiction", "desc");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void placeOrder_fullyFulfilled_noAdjustments() {
        Cart cart = cartWith(book("Dune", 10, new BigDecimal("15.99")), 2);
        when(cartRepository.findByUserId(any())).thenReturn(Optional.of(cart));

        OrderResponse response = orderService.placeOrder("user@test.com");

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(response.getAdjustments()).isEmpty();
        assertThat(response.getTotalPrice()).isEqualByComparingTo(new BigDecimal("31.98"));
    }

    @Test
    void placeOrder_partialStock_capsFulfilledAndReportsAdjustment() {
        Book book = book("Dune", 3, new BigDecimal("15.99"));
        Cart cart = cartWith(book, 5);
        when(cartRepository.findByUserId(any())).thenReturn(Optional.of(cart));

        OrderResponse response = orderService.placeOrder("user@test.com");

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(response.getAdjustments()).hasSize(1);
        assertThat(response.getAdjustments().get(0).getRequested()).isEqualTo(5);
        assertThat(response.getAdjustments().get(0).getFulfilled()).isEqualTo(3);
        assertThat(response.getAdjustments().get(0).getReason()).contains("3");
        assertThat(book.getStock()).isZero();
    }

    @Test
    void placeOrder_zeroStock_skipsItemAndReportsAdjustment() {
        Book outOfStock = book("Sold Out Book", 0, new BigDecimal("9.99"));
        Book inStock = book("Available Book", 5, new BigDecimal("12.99"));
        Cart cart = new Cart(user);
        cart.getItems().add(new CartItem(cart, outOfStock, 1));
        cart.getItems().add(new CartItem(cart, inStock, 2));
        when(cartRepository.findByUserId(any())).thenReturn(Optional.of(cart));

        OrderResponse response = orderService.placeOrder("user@test.com");

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getTitle()).isEqualTo("Available Book");
        assertThat(response.getAdjustments()).hasSize(1);
        assertThat(response.getAdjustments().get(0).getFulfilled()).isZero();
        assertThat(response.getAdjustments().get(0).getReason()).isEqualTo("Out of stock");
    }

    @Test
    void placeOrder_allItemsOutOfStock_throwsInvalidOperation() {
        Book outOfStock = book("Ghost Book", 0, new BigDecimal("9.99"));
        Cart cart = cartWith(outOfStock, 2);
        when(cartRepository.findByUserId(any())).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderService.placeOrder("user@test.com"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("None of the items in your cart are in stock");
    }

    @Test
    void placeOrder_emptyCart_throwsInvalidOperation() {
        when(cartRepository.findByUserId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder("user@test.com"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    void placeOrder_deductsStockExactly() {
        Book book = book("Dune", 10, new BigDecimal("15.99"));
        Cart cart = cartWith(book, 7);
        when(cartRepository.findByUserId(any())).thenReturn(Optional.of(cart));

        orderService.placeOrder("user@test.com");

        assertThat(book.getStock()).isEqualTo(3);
    }

    @Test
    void placeOrder_clearsCartAfterSuccess() {
        Cart cart = cartWith(book("Dune", 10, new BigDecimal("15.99")), 1);
        when(cartRepository.findByUserId(any())).thenReturn(Optional.of(cart));

        orderService.placeOrder("user@test.com");

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository, atLeastOnce()).save(cart);
    }

    @Test
    void placeOrder_priceSnapshotMatchesBookPrice() {
        BigDecimal price = new BigDecimal("29.95");
        Cart cart = cartWith(book("Expensive Book", 5, price), 1);
        when(cartRepository.findByUserId(any())).thenReturn(Optional.of(cart));

        OrderResponse response = orderService.placeOrder("user@test.com");

        assertThat(response.getItems().get(0).getPriceAtPurchase()).isEqualByComparingTo(price);
    }

    private Book book(String title, int stock, BigDecimal price) {
        return new Book(title, "Author", "desc", price, stock, null, category);
    }

    private Cart cartWith(Book book, int quantity) {
        Cart cart = new Cart(user);
        cart.getItems().add(new CartItem(cart, book, quantity));
        return cart;
    }
}
