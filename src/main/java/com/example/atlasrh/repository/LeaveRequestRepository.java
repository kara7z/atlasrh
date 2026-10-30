package com.example.atlasrh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import com.example.atlasrh.entity.LeaveRequest;

public interface LeaveRequestRepository extends ListCrudRepository<LeaveRequest, Long> {
  @Query("""
      SELECT lr.* FROM leave_requests lr
      JOIN employees e ON e.id = lr.employee_id
      WHERE e.manager_id = :managerId AND e.id <> :managerId AND lr.status = 'PENDING'
      ORDER BY lr.start_date, lr.id
      """)
  List<LeaveRequest> findPendingByManagerId(Long managerId);

  @Query("SELECT * FROM leave_requests WHERE id = :id FOR UPDATE")
  Optional<LeaveRequest> lockById(Long id);
}
