package com.example.atlasrh.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import com.example.atlasrh.entity.LeaveRequest;

public interface LeaveRequestRepository extends ListCrudRepository<LeaveRequest, Long> {
  Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

  Page<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, RequestStatus status, Pageable pageable);

  @Query("""
      SELECT COUNT(*) > 0 FROM leave_requests
      WHERE employee_id = :employeeId AND start_date <= :endDate AND end_date >= :startDate
      """)
  boolean existsOverlapping(Long employeeId, LocalDate startDate, LocalDate endDate);

  @Query("""
      SELECT * FROM leave_requests
      WHERE employee_id = :employeeId AND leave_type = :type AND status = 'APPROVED'
        AND start_date <= :yearEnd AND end_date >= :yearStart
      """)
  List<LeaveRequest> findApprovedForYear(Long employeeId, LeaveType type, LocalDate yearStart, LocalDate yearEnd);

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
