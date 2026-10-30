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

import com.example.atlasrh.dto.LeaveDecisionRequest;
import com.example.atlasrh.entity.Employee;
import com.example.atlasrh.entity.LeaveRequest;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.mapper.LeaveRequestMapper;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.repository.LeaveRequestRepository;
import com.example.atlasrh.service.impl.LeaveRequestServiceImpl;

@ExtendWith(MockitoExtension.class)
class LeaveDecisionServiceTests {
  @Mock
  private LeaveRequestRepository repository;
  @Mock
  private EmployeeRepository employeeRepository;
  private LeaveRequestService service;

  @BeforeEach
  void setUp() {
    service = new LeaveRequestServiceImpl(repository, Mappers.getMapper(LeaveRequestMapper.class), employeeRepository);
  }

  @Test
  void listsPendingDtosAndHandlesEmptyTeam() {
    when(employeeRepository.existsById(1L)).thenReturn(true);
    when(repository.findPendingByManagerId(1L)).thenReturn(List.of(leave(RequestStatus.PENDING)), List.of());
    assertThat(service.pendingForManager(1L)).extracting(item -> item.status()).containsExactly(RequestStatus.PENDING);
    assertThat(service.pendingForManager(1L)).isEmpty();
  }

  @Test
  void missingManagerReturns404() {
    assertThatThrownBy(() -> service.pendingForManager(99L)).isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.decide(3L, new LeaveDecisionRequest(99L, RequestStatus.APPROVED, null)))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).lockById(any());
  }

  @ParameterizedTest
  @EnumSource(value = RequestStatus.class, names = {"APPROVED", "REJECTED"})
  void approvesOrRejectsAndTrimsComment(RequestStatus decision) {
    var leave = leave(RequestStatus.PENDING);
    when(employeeRepository.existsById(1L)).thenReturn(true);
    when(repository.lockById(3L)).thenReturn(Optional.of(leave));
    when(employeeRepository.findById(2L)).thenReturn(Optional.of(Employee.builder().id(2L).managerId(1L).build()));
    when(repository.save(leave)).thenReturn(leave);
    var result = service.decide(3L, new LeaveDecisionRequest(1L, decision, "  Decision reason  "));
    assertThat(result.status()).isEqualTo(decision);
    assertThat(result.comment()).isEqualTo("Decision reason");
    assertThat(result.employeeId()).isEqualTo(2L);
  }

  @Test
  void approvalCommentIsOptional() {
    var leave = leave(RequestStatus.PENDING);
    when(employeeRepository.existsById(1L)).thenReturn(true);
    when(repository.lockById(3L)).thenReturn(Optional.of(leave));
    when(employeeRepository.findById(2L)).thenReturn(Optional.of(Employee.builder().id(2L).managerId(1L).build()));
    when(repository.save(leave)).thenReturn(leave);
    assertThat(service.decide(3L, new LeaveDecisionRequest(1L, RequestStatus.APPROVED, " ")).comment()).isNull();
  }

  @Test
  void rejectionRequiresNonblankComment() {
    for (String comment : new String[] {null, "", "  "}) {
      assertThatThrownBy(() -> service.decide(3L, new LeaveDecisionRequest(1L, RequestStatus.REJECTED, comment)))
          .isInstanceOf(BadRequestException.class);
    }
    verify(repository, never()).save(any());
  }

  @ParameterizedTest
  @EnumSource(value = RequestStatus.class, names = {"PENDING", "CANCELLED"})
  void rejectsInvalidTargetStates(RequestStatus status) {
    assertThatThrownBy(() -> service.decide(3L, new LeaveDecisionRequest(1L, status, "Reason")))
        .isInstanceOf(BadRequestException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void missingRequestOrNonTeamEmployeeReturns404() {
    when(employeeRepository.existsById(1L)).thenReturn(true);
    var request = new LeaveDecisionRequest(1L, RequestStatus.APPROVED, null);
    assertThatThrownBy(() -> service.decide(3L, request)).isInstanceOf(ResourceNotFoundException.class);
    when(repository.lockById(3L)).thenReturn(Optional.of(leave(RequestStatus.PENDING)));
    when(employeeRepository.findById(2L)).thenReturn(Optional.of(Employee.builder().id(2L).managerId(9L).build()));
    assertThatThrownBy(() -> service.decide(3L, request)).isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).save(any());
  }

  @ParameterizedTest
  @EnumSource(value = RequestStatus.class, names = {"APPROVED", "REJECTED", "CANCELLED"})
  void processedRequestsReturn409(RequestStatus status) {
    when(employeeRepository.existsById(1L)).thenReturn(true);
    when(repository.lockById(3L)).thenReturn(Optional.of(leave(status)));
    when(employeeRepository.findById(2L)).thenReturn(Optional.of(Employee.builder().id(2L).managerId(1L).build()));
    assertThatThrownBy(() -> service.decide(3L, new LeaveDecisionRequest(1L, RequestStatus.APPROVED, null)))
        .isInstanceOf(ConflictException.class);
    verify(repository, never()).save(any());
  }

  private LeaveRequest leave(RequestStatus status) {
    return LeaveRequest.builder().id(3L).employeeId(2L).status(status).build();
  }
}
