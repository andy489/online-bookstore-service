package com.bookstore.bookstore.dto.response;

import com.bookstore.bookstore.domain.Order;
import com.bookstore.bookstore.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class OrderResponse {

    private Long id;
    private OrderStatus status;
    private BigDecimal totalPrice;
    private List<OrderItemResponse> items;
    private List<OrderAdjustment> adjustments;
    private Instant createdAt;
    private Instant updatedAt;

    private OrderResponse() {}

    public static OrderResponse from(Order order) {
        OrderResponse r = new OrderResponse();
        r.id = order.getId();
        r.status = order.getStatus();
        r.totalPrice = order.getTotalPrice();
        r.items = order.getItems().stream().map(OrderItemResponse::from).toList();
        r.adjustments = Collections.emptyList();
        r.createdAt = order.getCreatedAt();
        r.updatedAt = order.getUpdatedAt();
        return r;
    }

    public static OrderResponse from(Order order, List<OrderAdjustment> adjustments) {
        OrderResponse r = from(order);
        r.adjustments = adjustments;
        return r;
    }

    public Long getId() { return id; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public List<OrderItemResponse> getItems() { return items; }
    public List<OrderAdjustment> getAdjustments() { return adjustments; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
