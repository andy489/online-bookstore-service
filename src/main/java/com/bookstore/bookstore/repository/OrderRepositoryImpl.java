package com.bookstore.bookstore.repository;

import com.bookstore.bookstore.domain.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepositoryImpl implements OrderRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Order> findById(Long id) {
        return em.createQuery(
                        "SELECT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.book WHERE o.id = :id",
                        Order.class)
                .setParameter("id", id)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }

    @Override
    public List<Order> findByUserId(Long userId, int page, int size) {
        List<Long> ids = em.createQuery(
                        "SELECT o.id FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC",
                        Long.class)
                .setParameter("userId", userId)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        return em.createQuery(
                        "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.book WHERE o.id IN :ids ORDER BY o.createdAt DESC",
                        Order.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public long countByUserId(Long userId) {
        return em.createQuery("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    @Override
    public List<Order> findAll(int page, int size) {
        List<Long> ids = em.createQuery(
                        "SELECT o.id FROM Order o ORDER BY o.createdAt DESC",
                        Long.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();

        if (ids.isEmpty()) {
            return List.of();
        }

        return em.createQuery(
                        "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.book WHERE o.id IN :ids ORDER BY o.createdAt DESC",
                        Order.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    @Override
    public long countAll() {
        return em.createQuery("SELECT COUNT(o) FROM Order o", Long.class)
                .getSingleResult();
    }

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            em.persist(order);
            return order;
        }
        return em.merge(order);
    }
}
