package com.bookstore.bookstore.dto.response;

import com.bookstore.bookstore.domain.Order;
import com.bookstore.bookstore.domain.OrderStatus;
import com.bookstore.bookstore.domain.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class OrderResponse {

    private Long id;
    private CustomerResponse customer;
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
        r.customer = CustomerResponse.from(order.getUser());
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
    public CustomerResponse getCustomer() { return customer; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public List<OrderItemResponse> getItems() { return items; }
    public List<OrderAdjustment> getAdjustments() { return adjustments; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** Lightweight customer view embedded in an order (who placed it). */
    public static class CustomerResponse {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;

        private CustomerResponse() {}

        static CustomerResponse from(User user) {
            CustomerResponse c = new CustomerResponse();
            c.id = user.getId();
            c.email = user.getEmail();
            c.firstName = user.getFirstName();
            c.lastName = user.getLastName();
            return c;
        }

        public Long getId() { return id; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
    }
}
