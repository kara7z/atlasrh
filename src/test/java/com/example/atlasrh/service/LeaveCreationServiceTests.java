package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.atlasrh.dto.LeaveCreateRequest;
import com.example.atlasrh.entity.LeaveRequest;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.mapper.LeaveRequestMapper;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.repository.LeaveRequestRepository;
import com.example.atlasrh.service.impl.LeaveBalancePolicy;
import com.example.atlasrh.service.impl.LeaveRequestServiceImpl;

@ExtendWith(MockitoExtension.class)
class LeaveCreationServiceTests {
  @Mock
  private LeaveRequestRepository repository;
  @Mock
  private EmployeeRepository employeeRepository;
  @Mock
  private LeaveBalancePolicy policy;
  private LeaveRequestService service;
  private final LocalDate start = LocalDate.of(2026, 11, 6);
  private final LocalDate end = LocalDate.of(2026, 11, 9);

  @BeforeEach
  void setUp() {
    service = new LeaveRequestServiceImpl(repository, Mappers.getMapper(LeaveRequestMapper.class),
        employeeRepository, policy);
  }

  @Test
  void createsPendingRequestWithComputedDaysAndServerControlledFields() {
    when(employeeRepository.lockById(1L)).thenReturn(Optional.of(1L));
    when(repository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
      LeaveRequest entity = invocation.getArgument(0);
      assertThat(entity.getId()).isNull();
      assertThat(entity.getEmployeeId()).isEqualTo(1L);
      assertThat(entity.getStatus()).isEqualTo(RequestStatus.PENDING);
      assertThat(entity.getComment()).isNull();
      return LeaveRequest.builder().id(3L).employeeId(1L).leaveType(entity.getLeaveType())
          .startDate(entity.getStartDate()).endDate(entity.getEndDate()).reason(entity.getReason())
          .status(entity.getStatus()).build();
    });
    var created = service.create(1L, request(start, end));
    assertThat(created.id()).isEqualTo(3L);
    assertThat(created.workingDays()).isEqualTo(2);
    assertThat(created.reason()).isEqualTo("Holiday");
    verify(policy).check(1L, LeaveType.ANNUAL, start, end);
  }

  @Test
  void rejectsReversedDatesAndWeekendOnlyRequests() {
    assertThatThrownBy(() -> service.create(1L, request(end, start))).isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> service.create(1L, request(start.plusDays(1), start.plusDays(2))))
        .isInstanceOf(BadRequestException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsMissingEmployee() {
    assertThatThrownBy(() -> service.create(99L, request(start, end))).isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsOverlapBeforeSaving() {
    when(employeeRepository.lockById(1L)).thenReturn(Optional.of(1L));
    when(repository.existsOverlapping(1L, start, end)).thenReturn(true);
    assertThatThrownBy(() -> service.create(1L, request(start, end))).isInstanceOf(BadRequestException.class);
    verify(repository, never()).save(any());
  }

  private LeaveCreateRequest request(LocalDate start, LocalDate end) {
    return new LeaveCreateRequest(LeaveType.ANNUAL, start, end, "  Holiday  ");
  }
}
