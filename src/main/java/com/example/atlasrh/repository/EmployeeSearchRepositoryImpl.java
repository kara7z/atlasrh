package com.example.atlasrh.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.atlasrh.entity.Employee;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class EmployeeSearchRepositoryImpl implements EmployeeSearchRepository {
  private static final char LIKE_ESCAPE = '\\';

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Page<Employee> search(String name, Long departmentId, Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Employee> query = builder.createQuery(Employee.class);
    Root<Employee> root = query.from(Employee.class);
    query.where(filters(builder, root, name, departmentId));
    query.orderBy(orders(builder, root, pageable.getSort()));

    List<Employee> content = entityManager.createQuery(query)
        .setFirstResult((int) pageable.getOffset())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
    return new PageImpl<>(content, pageable, count(name, departmentId));
  }

  private long count(String name, Long departmentId) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> query = builder.createQuery(Long.class);
    Root<Employee> root = query.from(Employee.class);
    query.select(builder.count(root));
    query.where(filters(builder, root, name, departmentId));
    return entityManager.createQuery(query).getSingleResult();
  }

  private Predicate[] filters(CriteriaBuilder builder, Root<Employee> root, String name, Long departmentId) {
    List<Predicate> predicates = new ArrayList<>();
    if (name != null && !name.isBlank()) {
      String pattern = "%" + escape(name.strip()).toLowerCase() + "%";
      predicates.add(builder.or(
          builder.like(builder.lower(root.get("firstName")), pattern, LIKE_ESCAPE),
          builder.like(builder.lower(root.get("lastName")), pattern, LIKE_ESCAPE)));
    }
    if (departmentId != null) {
      predicates.add(builder.equal(root.get("departmentId"), departmentId));
    }
    return predicates.toArray(Predicate[]::new);
  }

  private List<Order> orders(CriteriaBuilder builder, Root<Employee> root, Sort sort) {
    return sort.stream()
        .map(order -> order.isAscending()
            ? builder.asc(root.get(order.getProperty()))
            : builder.desc(root.get(order.getProperty())))
        .toList();
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
