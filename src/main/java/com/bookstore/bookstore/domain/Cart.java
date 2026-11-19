package com.bookstore.bookstore.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Cart() {}

    public Cart(User user) {
        this.user = user;
    }

    @PrePersist
    private void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public List<CartItem> getItems() { return items; }
    public Instant getCreatedAt() { return createdAt; }
}
