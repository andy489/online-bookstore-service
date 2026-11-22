package com.bookstore.bookstore.dto.response;

import com.bookstore.bookstore.domain.CartItem;

import java.math.BigDecimal;

public class CartItemResponse {

    private Long bookId;
    private String title;
    private String author;
    private String coverImageUrl;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;

    private CartItemResponse() {}

    public static CartItemResponse from(CartItem item) {
        CartItemResponse r = new CartItemResponse();
        r.bookId = item.getBook().getId();
        r.title = item.getBook().getTitle();
        r.author = item.getBook().getAuthor();
        r.coverImageUrl = item.getBook().getCoverImageUrl();
        r.price = item.getBook().getPrice();
        r.quantity = item.getQuantity();
        r.subtotal = item.getBook().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return r;
    }

    public Long getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public BigDecimal getPrice() { return price; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
}
