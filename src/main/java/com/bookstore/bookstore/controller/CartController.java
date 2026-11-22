package com.bookstore.bookstore.controller;

import com.bookstore.bookstore.dto.request.CartItemRequest;
import com.bookstore.bookstore.dto.request.UpdateCartItemRequest;
import com.bookstore.bookstore.dto.response.CartResponse;
import com.bookstore.bookstore.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Cart", description = "Shopping cart management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @Operation(summary = "Get current user's cart")
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(cartService.getCart(email));
    }

    @Operation(summary = "Add a book to the cart (increments quantity if already present)")
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthenticationPrincipal String email,
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(email, request.getBookId(), request.getQuantity()));
    }

    @Operation(summary = "Update quantity of a cart item")
    @PutMapping("/items/{bookId}")
    public ResponseEntity<CartResponse> updateItem(
            @AuthenticationPrincipal String email,
            @PathVariable Long bookId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(email, bookId, request.getQuantity()));
    }

    @Operation(summary = "Remove a book from the cart")
    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal String email,
            @PathVariable Long bookId) {
        cartService.removeItem(email, bookId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Clear the entire cart")
    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal String email) {
        cartService.clearCart(email);
        return ResponseEntity.noContent().build();
    }
}
