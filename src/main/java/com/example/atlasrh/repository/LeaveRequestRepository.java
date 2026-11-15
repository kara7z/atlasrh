package com.example.atlasrh.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.atlasrh.entity.LeaveRequest;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;

import jakarta.persistence.LockModeType;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
  Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);

  Page<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, RequestStatus status, Pageable pageable);

  @Query("""
      select count(lr) > 0 from LeaveRequest lr
      where lr.employeeId = :employeeId
        and lr.startDate <= :endDate
        and lr.endDate >= :startDate
      """)
  boolean existsOverlapping(@Param("employeeId") Long employeeId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  @Query("""
      select lr from LeaveRequest lr
      where lr.employeeId = :employeeId
        and lr.leaveType = :type
        and lr.status = com.example.atlasrh.enums.RequestStatus.APPROVED
        and lr.startDate <= :yearEnd
        and lr.endDate >= :yearStart
      """)
  List<LeaveRequest> findApprovedForYear(@Param("employeeId") Long employeeId,
      @Param("type") LeaveType type,
      @Param("yearStart") LocalDate yearStart,
      @Param("yearEnd") LocalDate yearEnd);

  @Query("""
      select lr from LeaveRequest lr, Employee e
      where lr.employeeId = e.id
        and e.managerId = :managerId
        and e.id <> :managerId
        and lr.status = com.example.atlasrh.enums.RequestStatus.PENDING
      order by lr.startDate, lr.id
      """)
  List<LeaveRequest> findPendingByManagerId(@Param("managerId") Long managerId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select lr from LeaveRequest lr where lr.id = :id")
  Optional<LeaveRequest> lockById(@Param("id") Long id);
}
