package com.bookstore.bookstore.unit;

import com.bookstore.bookstore.domain.*;
import com.bookstore.bookstore.dto.response.CartResponse;
import com.bookstore.bookstore.exception.ResourceNotFoundException;
import com.bookstore.bookstore.repository.BookRepository;
import com.bookstore.bookstore.repository.CartRepository;
import com.bookstore.bookstore.repository.UserRepository;
import com.bookstore.bookstore.service.CartService;
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
class CartServiceUnitTest {

    @Mock CartRepository cartRepository;
    @Mock BookRepository bookRepository;
    @Mock UserRepository userRepository;

    @InjectMocks CartService cartService;

    private User user;
    private Book book;
    private Cart cart;

    private static final Long BOOK_ID = 1L;

    @BeforeEach
    void setUp() throws Exception {
        user = new User("user@test.com", "hash", "Test", "User", Role.USER);
        Category category = new Category("Fiction", "desc");
        book = new Book("Dune", "Herbert", "desc", new BigDecimal("15.99"), 10, null, category);

        // Set id via reflection — Book has no public setter, JPA normally sets it
        var idField = Book.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(book, BOOK_ID);

        cart = new Cart(user);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(any())).thenReturn(Optional.of(cart));
        when(bookRepository.findActiveById(BOOK_ID)).thenReturn(Optional.of(book));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void addItem_newBook_addsItemToCart() {
        CartResponse response = cartService.addItem("user@test.com", BOOK_ID, 2);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void addItem_sameBookTwice_incrementsQuantity() {
        cartService.addItem("user@test.com", BOOK_ID, 2);
        CartResponse response = cartService.addItem("user@test.com", BOOK_ID, 3);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addItem_bookNotFound_throwsResourceNotFound() {
        when(bookRepository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem("user@test.com", 99L, 1))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateItem_changesQuantity() {
        cart.getItems().add(new CartItem(cart, book, 2));

        CartResponse response = cartService.updateItem("user@test.com", BOOK_ID, 5);

        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void updateItem_bookNotInCart_throwsResourceNotFound() {
        assertThatThrownBy(() -> cartService.updateItem("user@test.com", 99L, 5))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Item not in cart");
    }

    @Test
    void removeItem_removesFromCart() {
        cart.getItems().add(new CartItem(cart, book, 2));

        CartResponse response = cartService.removeItem("user@test.com", BOOK_ID);

        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void removeItem_bookNotInCart_throwsResourceNotFound() {
        assertThatThrownBy(() -> cartService.removeItem("user@test.com", 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCart_emptyCart_returnsEmptyItems() {
        CartResponse response = cartService.getCart("user@test.com");

        assertThat(response.getItems()).isEmpty();
    }

    @Test
    void clearCart_removesAllItems() {
        cart.getItems().add(new CartItem(cart, book, 3));

        cartService.clearCart("user@test.com");

        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository, atLeastOnce()).save(cart);
    }
}
