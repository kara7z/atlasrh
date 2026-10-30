package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.atlasrh.entity.LeaveRequest;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.mapper.LeaveRequestMapper;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.repository.LeaveRequestRepository;
import com.example.atlasrh.service.impl.LeaveBalancePolicy;
import com.example.atlasrh.service.impl.LeaveRequestServiceImpl;

@ExtendWith(MockitoExtension.class)
class LeaveTrackingServiceTests {
  @Mock
  private LeaveRequestRepository repository;
  @Mock
  private EmployeeRepository employees;
  @Mock
  private LeaveBalancePolicy policy;
  private LeaveRequestService service;

  @BeforeEach
  void setUp() {
    service = new LeaveRequestServiceImpl(repository, Mappers.getMapper(LeaveRequestMapper.class), employees, policy);
  }

  @Test
  void listsOnlyEmployeesRequestsWithDefaultSortAndMappedDtos() {
    var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startDate", "id"));
    when(employees.existsById(1L)).thenReturn(true);
    when(repository.findByEmployeeId(1L, pageable))
        .thenReturn(new PageImpl<>(List.of(leave(RequestStatus.PENDING)), pageable, 1));
    var result = service.listForEmployee(1L, null, PageRequest.of(0, 10));
    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().employeeId()).isEqualTo(1L);
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @ParameterizedTest
  @EnumSource(RequestStatus.class)
  void filtersEveryStatusAndAddsStableIdOrdering(RequestStatus status) {
    var pageable = PageRequest.of(1, 5, Sort.by("status").and(Sort.by("id")));
    when(employees.existsById(1L)).thenReturn(true);
    when(repository.findByEmployeeIdAndStatus(1L, status, pageable))
        .thenReturn(new PageImpl<>(List.of(leave(status)), pageable, 6));
    assertThat(service.listForEmployee(1L, status, PageRequest.of(1, 5, Sort.by("status")))
        .getContent().getFirst().status()).isEqualTo(status);
  }

  @Test
  void rejectsMissingEmployeeAndInvalidPageSettings() {
    assertThatThrownBy(() -> service.listForEmployee(99L, null, PageRequest.of(0, 10)))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.listForEmployee(1L, null, Pageable.unpaged()))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> service.listForEmployee(1L, null, PageRequest.of(0, 101)))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> service.listForEmployee(1L, null, PageRequest.of(0, 10, Sort.by("employee.password"))))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void cancellationChangesOnlyStatus() {
    var leave = leave(RequestStatus.PENDING);
    leave.setReason("Holiday");
    when(employees.existsById(1L)).thenReturn(true);
    when(repository.lockById(3L)).thenReturn(Optional.of(leave));
    when(repository.save(leave)).thenReturn(leave);
    var cancelled = service.cancel(1L, 3L);
    assertThat(cancelled.status()).isEqualTo(RequestStatus.CANCELLED);
    assertThat(cancelled.reason()).isEqualTo("Holiday");
    assertThat(cancelled.id()).isEqualTo(3L);
    verify(repository).save(leave);
  }

  @ParameterizedTest
  @EnumSource(value = RequestStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
  void onlyPendingRequestsCanBeCancelled(RequestStatus status) {
    when(employees.existsById(1L)).thenReturn(true);
    when(repository.lockById(3L)).thenReturn(Optional.of(leave(status)));
    assertThatThrownBy(() -> service.cancel(1L, 3L)).isInstanceOf(ConflictException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void missingOrWrongEmployeeRequestsReturn404WithoutSaving() {
    assertThatThrownBy(() -> service.cancel(99L, 3L)).isInstanceOf(ResourceNotFoundException.class);
    when(employees.existsById(1L)).thenReturn(true);
    assertThatThrownBy(() -> service.cancel(1L, 3L)).isInstanceOf(ResourceNotFoundException.class);
    var other = leave(RequestStatus.PENDING);
    other.setEmployeeId(2L);
    when(repository.lockById(3L)).thenReturn(Optional.of(other));
    assertThatThrownBy(() -> service.cancel(1L, 3L)).isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).save(any());
  }

  private LeaveRequest leave(RequestStatus status) {
    return LeaveRequest.builder().id(3L).employeeId(1L).status(status).build();
  }
}
