package com.bookstore.bookstore.dto.response;

import com.bookstore.bookstore.domain.OrderItem;

import java.math.BigDecimal;

public class OrderItemResponse {

    private Long bookId;
    private String title;
    private String author;
    private Integer quantity;
    private BigDecimal priceAtPurchase;
    private BigDecimal subtotal;

    private OrderItemResponse() {}

    public static OrderItemResponse from(OrderItem item) {
        OrderItemResponse r = new OrderItemResponse();
        r.bookId = item.getBook().getId();
        r.title = item.getBook().getTitle();
        r.author = item.getBook().getAuthor();
        r.quantity = item.getQuantity();
        r.priceAtPurchase = item.getPriceAtPurchase();
        r.subtotal = item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()));
        return r;
    }

    public Long getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getPriceAtPurchase() { return priceAtPurchase; }
    public BigDecimal getSubtotal() { return subtotal; }
}
