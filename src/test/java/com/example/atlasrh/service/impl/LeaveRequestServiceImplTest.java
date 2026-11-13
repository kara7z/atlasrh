package com.example.atlasrh.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.example.atlasrh.dto.LeaveCreateRequest;
import com.example.atlasrh.dto.LeaveCreatedDto;
import com.example.atlasrh.dto.LeaveDecisionRequest;
import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.entity.Employee;
import com.example.atlasrh.entity.LeaveRequest;
import com.example.atlasrh.enums.LeaveType;
import com.example.atlasrh.enums.RequestStatus;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.mapper.LeaveRequestMapper;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.repository.LeaveRequestRepository;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceImplTest {
  @Mock
  private LeaveRequestRepository repository;
  @Mock
  private LeaveRequestMapper mapper;
  @Mock
  private EmployeeRepository employeeRepository;
  @Mock
  private LeaveBalancePolicy balancePolicy;
  @InjectMocks
  private LeaveRequestServiceImpl service;

  private final LocalDate startDate = LocalDate.of(2026, 11, 2);
  private final LocalDate endDate = LocalDate.of(2026, 11, 6);
  private final LeaveRequest leave = LeaveRequest.builder()
      .id(1L).employeeId(2L).leaveType(LeaveType.ANNUAL)
      .startDate(startDate).endDate(endDate).reason("Family trip")
      .status(RequestStatus.PENDING).build();
  private final LeaveRequestDto leaveDto = new LeaveRequestDto(
      1L, 2L, LeaveType.ANNUAL, startDate, endDate, "Family trip", RequestStatus.PENDING, null);
  private final Employee teamMember = Employee.builder().id(2L).managerId(5L).build();

  @BeforeEach
  void printTestMethodName(TestInfo testInfo) {
    System.out.println("\nRunning test: " + testInfo.getTestMethod().orElseThrow().getName());
  }

  @Nested
  class ReadOperations {
    @Test
    void findAllReturnsLeaveRequests() {
      when(repository.findAll()).thenReturn(List.of(leave));
      when(mapper.toDto(leave)).thenReturn(leaveDto);

      List<LeaveRequestDto> result = service.findAll();
      System.out.println("findAll result: " + result);

      assertThat(result).containsExactly(leaveDto);
      verify(repository).findAll();
    }

    @Test
    void findByIdReturnsLeaveRequest() {
      when(repository.findById(1L)).thenReturn(Optional.of(leave));
      when(mapper.toDto(leave)).thenReturn(leaveDto);

      Optional<LeaveRequestDto> result = service.findById(1L);
      System.out.println("findById result: " + result);

      assertThat(result).contains(leaveDto);
      verify(repository).findById(1L);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
      when(repository.findById(99L)).thenReturn(Optional.empty());

      Optional<LeaveRequestDto> result = service.findById(99L);
      System.out.println("findById missing result: " + result);

      assertThat(result).isEmpty();
      verify(repository).findById(99L);
    }
  }

  @Nested
  class ListingOperations {
    @Test
    void listForEmployeeWithoutStatusReturnsAllRequests() {
      Pageable pageable = PageRequest.of(0, 10, Sort.by("leaveType"));
      Pageable stable = PageRequest.of(0, 10, Sort.by("leaveType").and(Sort.by("id")));
      when(employeeRepository.existsById(2L)).thenReturn(true);
      when(repository.findByEmployeeId(2L, stable))
          .thenReturn(new PageImpl<>(List.of(leave), stable, 1));
      when(mapper.toDto(leave)).thenReturn(leaveDto);

      Page<LeaveRequestDto> result = service.listForEmployee(2L, null, pageable);
      System.out.println("listForEmployee result: " + result.getContent());

      assertThat(result.getContent()).containsExactly(leaveDto);
      assertThat(result.getSort()).isEqualTo(stable.getSort());
      verify(repository).findByEmployeeId(2L, stable);
    }

    @Test
    void listForEmployeeWithStatusFiltersByStatus() {
      Pageable pageable = PageRequest.of(0, 10, Sort.by("startDate"));
      Pageable stable = PageRequest.of(0, 10, Sort.by("startDate").and(Sort.by("id")));
      when(employeeRepository.existsById(2L)).thenReturn(true);
      when(repository.findByEmployeeIdAndStatus(2L, RequestStatus.PENDING, stable))
          .thenReturn(new PageImpl<>(List.of(leave), stable, 1));
      when(mapper.toDto(leave)).thenReturn(leaveDto);

      Page<LeaveRequestDto> result = service.listForEmployee(2L, RequestStatus.PENDING, pageable);
      System.out.println("listForEmployee status result: " + result.getContent());

      assertThat(result.getContent()).containsExactly(leaveDto);
      verify(repository).findByEmployeeIdAndStatus(2L, RequestStatus.PENDING, stable);
    }

    @Test
    void listForEmployeeDefaultsToNewestFirst() {
      Pageable pageable = PageRequest.of(0, 10);
      Pageable stable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startDate", "id"));
      when(employeeRepository.existsById(2L)).thenReturn(true);
      when(repository.findByEmployeeId(2L, stable))
          .thenReturn(new PageImpl<>(List.of(leave), stable, 1));
      when(mapper.toDto(leave)).thenReturn(leaveDto);

      Page<LeaveRequestDto> result = service.listForEmployee(2L, null, pageable);
      System.out.println("listForEmployee default sort result: " + result.getSort());

      assertThat(result.getSort()).isEqualTo(stable.getSort());
      verify(repository).findByEmployeeId(2L, stable);
    }

    @Test
    void listForEmployeeKeepsExplicitIdSort() {
      Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
      when(employeeRepository.existsById(2L)).thenReturn(true);
      when(repository.findByEmployeeId(2L, pageable))
          .thenReturn(new PageImpl<>(List.of(), pageable, 0));

      Page<LeaveRequestDto> result = service.listForEmployee(2L, null, pageable);
      System.out.println("listForEmployee explicit id sort result: " + result.getSort());

      assertThat(result.getSort()).isEqualTo(pageable.getSort());
      verify(repository).findByEmployeeId(2L, pageable);
    }
  }

  @Nested
  class ListingValidation {
    @Test
    void listRejectsUnpagedRequest() {
      assertThatThrownBy(() -> service.listForEmployee(2L, null, Pageable.unpaged()))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("page size");
    }

    @Test
    void listRejectsPageSizeAboveOneHundred() {
      Pageable pageable = PageRequest.of(0, 101, Sort.by("startDate"));

      assertThatThrownBy(() -> service.listForEmployee(2L, null, pageable))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("page size");
    }

    @Test
    void listRejectsUnsupportedSortField() {
      Pageable pageable = PageRequest.of(0, 10, Sort.by("employeeId"));

      assertThatThrownBy(() -> service.listForEmployee(2L, null, pageable))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Unsupported leave request sort field: employeeId");
    }

    @Test
    void listRejectsMissingEmployee() {
      Pageable pageable = PageRequest.of(0, 10, Sort.by("startDate"));
      when(employeeRepository.existsById(99L)).thenReturn(false);

      assertThatThrownBy(() -> service.listForEmployee(99L, null, pageable))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Employee not found: 99");
    }
  }

  @Nested
  class CreateOperations {
    @Test
    void createSavesPendingLeaveRequest() {
      LeaveCreateRequest request = new LeaveCreateRequest(
          LeaveType.ANNUAL, startDate, endDate, "  Family trip  ");
      LeaveRequestDto expectedDto = new LeaveRequestDto(
          null, 2L, LeaveType.ANNUAL, startDate, endDate, "Family trip", RequestStatus.PENDING, null);
      LeaveCreatedDto createdDto = new LeaveCreatedDto(
          1L, 2L, LeaveType.ANNUAL, startDate, endDate, "Family trip", RequestStatus.PENDING, null, 5);
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.existsOverlapping(2L, startDate, endDate)).thenReturn(false);
      when(mapper.toEntity(expectedDto)).thenReturn(leave);
      when(repository.save(leave)).thenReturn(leave);
      when(mapper.toCreatedDto(leave)).thenReturn(createdDto);

      LeaveCreatedDto result = service.create(2L, request);
      System.out.println("create result: " + result);

      assertThat(result).isEqualTo(createdDto);
      verify(balancePolicy).check(2L, LeaveType.ANNUAL, startDate, endDate);
      verify(repository).save(leave);
    }

    @Test
    void createRejectsEndDateBeforeStartDate() {
      LeaveCreateRequest request = new LeaveCreateRequest(
          LeaveType.ANNUAL, endDate, startDate, "Family trip");

      assertThatThrownBy(() -> service.create(2L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("End date must not be before start date");
    }

    @Test
    void createRejectsRequestWithoutWorkingDays() {
      LocalDate saturday = LocalDate.of(2026, 11, 14);
      LocalDate sunday = LocalDate.of(2026, 11, 15);
      LeaveCreateRequest request = new LeaveCreateRequest(
          LeaveType.ANNUAL, saturday, sunday, "Weekend");

      assertThatThrownBy(() -> service.create(2L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("at least one working day");
    }

    @Test
    void createRejectsMissingEmployee() {
      LeaveCreateRequest request = new LeaveCreateRequest(
          LeaveType.ANNUAL, startDate, endDate, "Family trip");
      when(employeeRepository.lockById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.create(99L, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Employee not found: 99");
    }

    @Test
    void createRejectsOverlappingLeaveRequest() {
      LeaveCreateRequest request = new LeaveCreateRequest(
          LeaveType.ANNUAL, startDate, endDate, "Family trip");
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.existsOverlapping(2L, startDate, endDate)).thenReturn(true);

      assertThatThrownBy(() -> service.create(2L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("overlaps an existing request");
      verify(repository, never()).save(any());
    }

    @Test
    void createRejectsRequestExceedingBalance() {
      LeaveCreateRequest request = new LeaveCreateRequest(
          LeaveType.ANNUAL, startDate, endDate, "Family trip");
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.existsOverlapping(2L, startDate, endDate)).thenReturn(false);
      doThrow(new BadRequestException("Leave request exceeds the remaining balance for 2026"))
          .when(balancePolicy).check(2L, LeaveType.ANNUAL, startDate, endDate);

      assertThatThrownBy(() -> service.create(2L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("exceeds the remaining balance");
      verify(repository, never()).save(any());
    }
  }

  @Nested
  class CancelOperations {
    @Test
    void cancelMarksPendingRequestAsCancelled() {
      LeaveRequestDto cancelledDto = new LeaveRequestDto(
          1L, 2L, LeaveType.ANNUAL, startDate, endDate, "Family trip", RequestStatus.CANCELLED, null);
      when(employeeRepository.existsById(2L)).thenReturn(true);
      when(repository.lockById(1L)).thenReturn(Optional.of(leave));
      when(repository.save(leave)).thenReturn(leave);
      when(mapper.toDto(leave)).thenReturn(cancelledDto);

      LeaveRequestDto result = service.cancel(2L, 1L);
      System.out.println("cancel result: " + result);

      assertThat(result).isEqualTo(cancelledDto);
      assertThat(leave.getStatus()).isEqualTo(RequestStatus.CANCELLED);
      verify(repository).save(leave);
    }

    @Test
    void cancelRejectsMissingEmployee() {
      when(employeeRepository.existsById(99L)).thenReturn(false);

      assertThatThrownBy(() -> service.cancel(99L, 1L))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Employee not found: 99");
    }

    @Test
    void cancelRejectsMissingLeaveRequest() {
      when(employeeRepository.existsById(2L)).thenReturn(true);
      when(repository.lockById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.cancel(2L, 99L))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Leave request not found for employee: 2");
    }

    @Test
    void cancelRejectsRequestOwnedByAnotherEmployee() {
      leave.setEmployeeId(3L);
      when(employeeRepository.existsById(2L)).thenReturn(true);
      when(repository.lockById(1L)).thenReturn(Optional.of(leave));

      assertThatThrownBy(() -> service.cancel(2L, 1L))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Leave request not found for employee: 2");
    }

    @Test
    void cancelRejectsNonPendingRequest() {
      leave.setStatus(RequestStatus.APPROVED);
      when(employeeRepository.existsById(2L)).thenReturn(true);
      when(repository.lockById(1L)).thenReturn(Optional.of(leave));

      assertThatThrownBy(() -> service.cancel(2L, 1L))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining("Only PENDING leave requests can be cancelled");
      verify(repository, never()).save(any());
    }
  }

  @Nested
  class DecisionValidation {
    @Test
    void decideRejectsInvalidDecision() {
      LeaveDecisionRequest request = new LeaveDecisionRequest(5L, RequestStatus.PENDING, null);

      assertThatThrownBy(() -> service.decide(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Decision must be APPROVED or REJECTED");
    }

    @Test
    void decideRejectsRejectionWithoutComment() {
      LeaveDecisionRequest request = new LeaveDecisionRequest(5L, RequestStatus.REJECTED, null);

      assertThatThrownBy(() -> service.decide(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("A comment is required when rejecting");
    }

    @Test
    void decideRejectsRejectionWithBlankComment() {
      LeaveDecisionRequest request = new LeaveDecisionRequest(5L, RequestStatus.REJECTED, "   ");

      assertThatThrownBy(() -> service.decide(1L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("A comment is required when rejecting");
    }

    @Test
    void decideRejectsMissingManager() {
      LeaveDecisionRequest request = new LeaveDecisionRequest(5L, RequestStatus.APPROVED, null);
      when(employeeRepository.existsById(5L)).thenReturn(false);

      assertThatThrownBy(() -> service.decide(1L, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Manager not found: 5");
    }

    @Test
    void decideRejectsMissingLeaveRequest() {
      LeaveDecisionRequest request = new LeaveDecisionRequest(5L, RequestStatus.APPROVED, null);
      when(employeeRepository.existsById(5L)).thenReturn(true);
      when(repository.lockById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.decide(99L, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Leave request not found: 99");
    }

    @Test
    void decideRejectsRequestOutsideManagerTeam() {
      LeaveDecisionRequest request = new LeaveDecisionRequest(5L, RequestStatus.APPROVED, null);
      teamMember.setManagerId(9L);
      when(employeeRepository.existsById(5L)).thenReturn(true);
      when(repository.lockById(1L)).thenReturn(Optional.of(leave));
      when(employeeRepository.findById(2L)).thenReturn(Optional.of(teamMember));

      assertThatThrownBy(() -> service.decide(1L, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("not found in manager's team");
    }

    @Test
    void decideRejectsNonPendingRequest() {
      LeaveDecisionRequest request = new LeaveDecisionRequest(5L, RequestStatus.APPROVED, null);
      leave.setStatus(RequestStatus.REJECTED);
      when(employeeRepository.existsById(5L)).thenReturn(true);
      when(repository.lockById(1L)).thenReturn(Optional.of(leave));
      when(employeeRepository.findById(2L)).thenReturn(Optional.of(teamMember));

      assertThatThrownBy(() -> service.decide(1L, request))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining("Only PENDING leave requests can be decided");
      verify(repository, never()).save(any());
    }
  }

  @Nested
  class DecisionWorkflow {
    @Test
    void approveChecksBalanceAndSavesDecision() {
      LeaveDecisionRequest request = new LeaveDecisionRequest(5L, RequestStatus.APPROVED, "  Looks good  ");
      LeaveRequestDto approvedDto = new LeaveRequestDto(
          1L, 2L, LeaveType.ANNUAL, startDate, endDate, "Family trip", RequestStatus.APPROVED, "Looks good");
      when(employeeRepository.existsById(5L)).thenReturn(true);
      when(repository.lockById(1L)).thenReturn(Optional.of(leave));
      when(employeeRepository.findById(2L)).thenReturn(Optional.of(teamMember));
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.save(leave)).thenReturn(leave);
      when(mapper.toDto(leave)).thenReturn(approvedDto);

      LeaveRequestDto result = service.decide(1L, request);
      System.out.println("approve result: " + result);

      assertThat(result).isEqualTo(approvedDto);
      assertThat(leave.getStatus()).isEqualTo(RequestStatus.APPROVED);
      assertThat(leave.getComment()).isEqualTo("Looks good");
      verify(balancePolicy).check(2L, LeaveType.ANNUAL, startDate, endDate);
      verify(employeeRepository).lockById(2L);
      verify(repository).save(leave);
    }

    @Test
    void rejectSavesCommentWithoutCheckingBalance() {
      LeaveDecisionRequest request = new LeaveDecisionRequest(
          5L, RequestStatus.REJECTED, "  Insufficient coverage  ");
      LeaveRequestDto rejectedDto = new LeaveRequestDto(
          1L, 2L, LeaveType.ANNUAL, startDate, endDate, "Family trip",
          RequestStatus.REJECTED, "Insufficient coverage");
      when(employeeRepository.existsById(5L)).thenReturn(true);
      when(repository.lockById(1L)).thenReturn(Optional.of(leave));
      when(employeeRepository.findById(2L)).thenReturn(Optional.of(teamMember));
      when(repository.save(leave)).thenReturn(leave);
      when(mapper.toDto(leave)).thenReturn(rejectedDto);

      LeaveRequestDto result = service.decide(1L, request);
      System.out.println("reject result: " + result);

      assertThat(result).isEqualTo(rejectedDto);
      assertThat(leave.getStatus()).isEqualTo(RequestStatus.REJECTED);
      assertThat(leave.getComment()).isEqualTo("Insufficient coverage");
      verifyNoInteractions(balancePolicy);
      verify(repository).save(leave);
    }

    @Test
    void approveNormalizesBlankCommentToNull() {
      LeaveDecisionRequest request = new LeaveDecisionRequest(5L, RequestStatus.APPROVED, "   ");
      LeaveRequestDto approvedDto = new LeaveRequestDto(
          1L, 2L, LeaveType.ANNUAL, startDate, endDate, "Family trip", RequestStatus.APPROVED, null);
      when(employeeRepository.existsById(5L)).thenReturn(true);
      when(repository.lockById(1L)).thenReturn(Optional.of(leave));
      when(employeeRepository.findById(2L)).thenReturn(Optional.of(teamMember));
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.save(leave)).thenReturn(leave);
      when(mapper.toDto(leave)).thenReturn(approvedDto);

      LeaveRequestDto result = service.decide(1L, request);
      System.out.println("approve blank comment result: " + result);

      assertThat(result).isEqualTo(approvedDto);
      assertThat(leave.getComment()).isNull();
    }
  }

  @Nested
  class ManagerOperations {
    @Test
    void pendingForManagerReturnsTeamRequests() {
      when(employeeRepository.existsById(5L)).thenReturn(true);
      when(repository.findPendingByManagerId(5L)).thenReturn(List.of(leave));
      when(mapper.toDto(leave)).thenReturn(leaveDto);

      List<LeaveRequestDto> result = service.pendingForManager(5L);
      System.out.println("pendingForManager result: " + result);

      assertThat(result).containsExactly(leaveDto);
      verify(repository).findPendingByManagerId(5L);
    }

    @Test
    void pendingForManagerRejectsMissingManager() {
      when(employeeRepository.existsById(99L)).thenReturn(false);

      assertThatThrownBy(() -> service.pendingForManager(99L))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Manager not found: 99");
    }
  }
}
