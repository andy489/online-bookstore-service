package com.bookstore.bookstore.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    protected OrderItem() {}

    public OrderItem(Order order, Book book, Integer quantity, BigDecimal priceAtPurchase) {
        this.order = order;
        this.book = book;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Book getBook() { return book; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getPriceAtPurchase() { return priceAtPurchase; }
}
