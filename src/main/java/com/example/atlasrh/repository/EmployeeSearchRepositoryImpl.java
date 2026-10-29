package com.example.atlasrh.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;

import com.example.atlasrh.entity.Employee;

public class EmployeeSearchRepositoryImpl implements EmployeeSearchRepository {
  private final JdbcAggregateTemplate template;

  public EmployeeSearchRepositoryImpl(JdbcAggregateTemplate template) {
    this.template = template;
  }

  @Override
  public Page<Employee> search(String name, Long departmentId, Pageable pageable) {
    Criteria criteria = Criteria.empty();
    if (name != null && !name.isBlank()) {
      // Treat SQL LIKE wildcards as literal characters in user input.
      String pattern = "%" + name.strip().replace("\\", "\\\\").replace("%", "\\%")
          .replace("_", "\\_") + "%";
      criteria = criteria.and(Criteria.where("firstName").like(pattern).ignoreCase(true)
          .or(Criteria.where("lastName").like(pattern).ignoreCase(true)));
    }
    if (departmentId != null) {
      criteria = criteria.and("departmentId").is(departmentId);
    }
    return template.findAll(Query.query(criteria), Employee.class, pageable);
  }
}
