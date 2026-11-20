package com.bookstore.bookstore.repository;

import com.bookstore.bookstore.domain.Category;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepositoryImpl implements CategoryRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(em.find(Category.class, id));
    }

    @Override
    public Optional<Category> findByName(String name) {
        return em.createQuery("SELECT c FROM Category c WHERE c.name = :name", Category.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst();
    }

    @Override
    public List<Category> findAll() {
        return em.createQuery("SELECT c FROM Category c ORDER BY c.name", Category.class)
                .getResultList();
    }

    @Override
    public boolean existsByName(String name) {
        Long count = em.createQuery("SELECT COUNT(c) FROM Category c WHERE c.name = :name", Long.class)
                .setParameter("name", name)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Long id) {
        Long count = em.createQuery(
                        "SELECT COUNT(c) FROM Category c WHERE c.name = :name AND c.id <> :id", Long.class)
                .setParameter("name", name)
                .setParameter("id", id)
                .getSingleResult();
        return count > 0;
    }

    @Override
    public Category save(Category category) {
        if (category.getId() == null) {
            em.persist(category);
            return category;
        }
        return em.merge(category);
    }

    @Override
    public void delete(Category category) {
        em.remove(em.contains(category) ? category : em.merge(category));
    }
}
