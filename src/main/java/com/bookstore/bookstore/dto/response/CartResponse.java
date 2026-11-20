package com.bookstore.bookstore.dto.response;

import com.bookstore.bookstore.domain.Cart;

import java.math.BigDecimal;
import java.util.List;

public class CartResponse {

    private Long id;
    private List<CartItemResponse> items;
    private BigDecimal total;

    private CartResponse() {}

    public static CartResponse from(Cart cart) {
        CartResponse r = new CartResponse();
        r.id = cart.getId();
        r.items = cart.getItems().stream().map(CartItemResponse::from).toList();
        r.total = r.items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return r;
    }

    public Long getId() { return id; }
    public List<CartItemResponse> getItems() { return items; }
    public BigDecimal getTotal() { return total; }
}
