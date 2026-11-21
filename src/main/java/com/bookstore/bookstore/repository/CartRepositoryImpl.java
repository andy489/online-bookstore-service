package com.bookstore.bookstore.repository;

import com.bookstore.bookstore.domain.Cart;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CartRepositoryImpl implements CartRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Cart> findByUserId(Long userId) {
        return em.createQuery(
                        "SELECT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.book WHERE c.user.id = :userId",
                        Cart.class)
                .setParameter("userId", userId)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public Cart save(Cart cart) {
        if (cart.getId() == null) {
            em.persist(cart);
            return cart;
        }
        return em.merge(cart);
    }
}
