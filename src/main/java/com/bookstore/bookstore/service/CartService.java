package com.bookstore.bookstore.service;

import com.bookstore.bookstore.domain.Book;
import com.bookstore.bookstore.domain.Cart;
import com.bookstore.bookstore.domain.CartItem;
import com.bookstore.bookstore.domain.User;
import com.bookstore.bookstore.dto.response.CartResponse;
import com.bookstore.bookstore.exception.ResourceNotFoundException;
import com.bookstore.bookstore.repository.BookRepository;
import com.bookstore.bookstore.repository.CartRepository;
import com.bookstore.bookstore.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository, BookRepository bookRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CartResponse getCart(String email) {
        User user = resolveUser(email);
        return CartResponse.from(findOrCreate(user));
    }

    @Transactional
    public CartResponse addItem(String email, Long bookId, Integer quantity) {
        User user = resolveUser(email);
        Cart cart = findOrCreate(user);
        Book book = bookRepository.findActiveById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

        if (book.getStock() == 0) {
            throw new com.bookstore.bookstore.exception.InvalidOperationException(
                    "'" + book.getTitle() + "' is currently out of stock");
        }

        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getBook().getId().equals(bookId))
                .findFirst()
                .orElse(null);

        if (existing == null && cart.getItems().size() >= 100) {
            throw new com.bookstore.bookstore.exception.InvalidOperationException(
                    "Cart cannot contain more than 100 distinct items");
        }

        int existingQuantity = existing != null ? existing.getQuantity() : 0;
        if (existingQuantity + quantity > book.getStock()) {
            throw new com.bookstore.bookstore.exception.InvalidOperationException(
                    "Requested quantity exceeds available stock for '" + book.getTitle() + "'");
        }

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            cart.getItems().add(new CartItem(cart, book, quantity));
        }

        cartRepository.save(cart);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateItem(String email, Long bookId, Integer quantity) {
        Cart cart = getCartOrThrow(resolveUser(email));
        CartItem item = findItemOrThrow(cart, bookId);
        Book book = item.getBook();
        if (quantity > book.getStock()) {
            throw new com.bookstore.bookstore.exception.InvalidOperationException(
                    "Requested quantity exceeds available stock for '" + book.getTitle() + "'");
        }
        item.setQuantity(quantity);
        cartRepository.save(cart);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse removeItem(String email, Long bookId) {
        Cart cart = getCartOrThrow(resolveUser(email));
        cart.getItems().remove(findItemOrThrow(cart, bookId));
        cartRepository.save(cart);
        return CartResponse.from(cart);
    }

    @Transactional
    public void clearCart(String email) {
        User user = resolveUser(email);
        cartRepository.findByUserId(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    Cart findOrCreateForUser(User user) {
        return findOrCreate(user);
    }

    private Cart findOrCreate(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(new Cart(user)));
    }

    private Cart getCartOrThrow(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    }

    private CartItem findItemOrThrow(Cart cart, Long bookId) {
        return cart.getItems().stream()
                .filter(i -> i.getBook().getId().equals(bookId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item not in cart: " + bookId));
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
