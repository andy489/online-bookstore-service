package com.bookstore.bookstore.repository;

import com.bookstore.bookstore.domain.Book;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class BookRepositoryImpl implements BookRepository {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(em.find(Book.class, id));
    }

    @Override
    public Optional<Book> findActiveById(Long id) {
        return em.createQuery(
                        "SELECT b FROM Book b WHERE b.id = :id AND b.deleted = false", Book.class)
                .setParameter("id", id)
                .getResultStream()
                .findFirst();
    }

    @Override
    public Book save(Book book) {
        if (book.getId() == null) {
            em.persist(book);
            return book;
        }
        return em.merge(book);
    }

    @Override
    public void delete(Book book) {
        em.remove(em.contains(book) ? book : em.merge(book));
    }

    @Override
    public List<Book> search(String query, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                             int page, int size, String sortBy, String sortDir) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Book> cq = cb.createQuery(Book.class);
        Root<Book> root = cq.from(Book.class);
        root.fetch("category", JoinType.LEFT);

        cq.where(buildPredicates(cb, root, query, categoryId, minPrice, maxPrice)
                .toArray(new Predicate[0]));

        Path<?> sortPath = "author".equals(sortBy) ? root.get("author") :
                "price".equals(sortBy) ? root.get("price") : root.get("title");
        cq.orderBy("desc".equalsIgnoreCase(sortDir) ? cb.desc(sortPath) : cb.asc(sortPath));

        return em.createQuery(cq)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
    }

    @Override
    public long countSearch(String query, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Book> root = cq.from(Book.class);

        cq.select(cb.count(root));
        cq.where(buildPredicates(cb, root, query, categoryId, minPrice, maxPrice)
                .toArray(new Predicate[0]));

        return em.createQuery(cq).getSingleResult();
    }

    private List<Predicate> buildPredicates(CriteriaBuilder cb, Root<Book> root,
                                            String query, Long categoryId,
                                            BigDecimal minPrice, BigDecimal maxPrice) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isFalse(root.get("deleted")));

        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("author")), pattern)
            ));
        }
        if (categoryId != null) {
            predicates.add(cb.equal(root.get("category").get("id"), categoryId));
        }
        if (minPrice != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        return predicates;
    }
}
