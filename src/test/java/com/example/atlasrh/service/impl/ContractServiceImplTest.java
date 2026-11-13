package com.example.atlasrh.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
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

import com.example.atlasrh.dto.ContractDto;
import com.example.atlasrh.dto.ContractHistoryDto;
import com.example.atlasrh.dto.ContractRequest;
import com.example.atlasrh.dto.EndContractRequest;
import com.example.atlasrh.entity.Contract;
import com.example.atlasrh.enums.ContractStatus;
import com.example.atlasrh.enums.ContractType;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.mapper.ContractMapper;
import com.example.atlasrh.repository.ContractRepository;
import com.example.atlasrh.repository.EmployeeRepository;

@ExtendWith(MockitoExtension.class)
class ContractServiceImplTest {
  @Mock
  private ContractRepository repository;
  @Mock
  private ContractMapper mapper;
  @Mock
  private EmployeeRepository employeeRepository;
  @InjectMocks
  private ContractServiceImpl service;

  private final LocalDate startDate = LocalDate.of(2020, 1, 1);
  private final BigDecimal salary = new BigDecimal("12000.00");
  private final Contract contract = Contract.builder()
      .id(1L).type(ContractType.PERMANENT).startDate(startDate)
      .grossSalary(salary).employeeId(2L).build();
  private final ContractDto contractDto = new ContractDto(
      1L, ContractType.PERMANENT, startDate, null, salary, 2L);

  @BeforeEach
  void printTestMethodName(TestInfo testInfo) {
    System.out.println("\nRunning test: " + testInfo.getTestMethod().orElseThrow().getName());
  }

  @Nested
  class ReadOperations {
    @Test
    void findAllReturnsContracts() {
      when(repository.findAll()).thenReturn(List.of(contract));
      when(mapper.toDto(contract)).thenReturn(contractDto);

      List<ContractDto> result = service.findAll();
      System.out.println("findAll result: " + result);

      assertThat(result).containsExactly(contractDto);
      verify(repository).findAll();
    }

    @Test
    void findByIdReturnsContract() {
      when(repository.findById(1L)).thenReturn(Optional.of(contract));
      when(mapper.toDto(contract)).thenReturn(contractDto);

      Optional<ContractDto> result = service.findById(1L);
      System.out.println("findById result: " + result);

      assertThat(result).contains(contractDto);
      verify(repository).findById(1L);
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
      when(repository.findById(99L)).thenReturn(Optional.empty());

      Optional<ContractDto> result = service.findById(99L);
      System.out.println("findById missing result: " + result);

      assertThat(result).isEmpty();
      verify(repository).findById(99L);
    }

    @Test
    void historyReturnsEmployeeContracts() {
      ContractHistoryDto historyDto = new ContractHistoryDto(
          1L, ContractType.PERMANENT, startDate, null, salary, 2L, ContractStatus.ACTIVE);
      when(employeeRepository.existsById(2L)).thenReturn(true);
      when(repository.findByEmployeeIdOrderByStartDateDescIdDesc(2L)).thenReturn(List.of(contract));
      when(mapper.toHistoryDto(eq(contract), any(LocalDate.class))).thenReturn(historyDto);

      List<ContractHistoryDto> result = service.history(2L);
      System.out.println("history result: " + result);

      assertThat(result).containsExactly(historyDto);
      verify(employeeRepository).existsById(2L);
      verify(repository).findByEmployeeIdOrderByStartDateDescIdDesc(2L);
    }

    @Test
    void historyRejectsMissingEmployee() {
      when(employeeRepository.existsById(99L)).thenReturn(false);

      assertThatThrownBy(() -> service.history(99L))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Employee not found: 99");
    }
  }

  @Nested
  class CreateOperations {
    @Test
    void createSavesPermanentContractForEmployee() {
      ContractRequest request = new ContractRequest(ContractType.PERMANENT, startDate, null, salary);
      ContractDto newContractDto = new ContractDto(null, request.type(), startDate, null, salary, 2L);
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.existsOverlapping(2L, startDate, null)).thenReturn(false);
      when(mapper.toEntity(newContractDto)).thenReturn(contract);
      when(repository.save(contract)).thenReturn(contract);
      when(mapper.toDto(contract)).thenReturn(contractDto);

      ContractDto result = service.create(2L, request);
      System.out.println("create result: " + result);

      assertThat(result).isEqualTo(contractDto);
      verify(employeeRepository).lockById(2L);
      verify(repository).existsOverlapping(2L, startDate, null);
      verify(repository).save(contract);
    }

    @Test
    void createSavesFixedTermContractWithEndDate() {
      LocalDate endDate = startDate.plusYears(1);
      ContractRequest request = new ContractRequest(ContractType.FIXED_TERM, startDate, endDate, salary);
      Contract fixedTerm = Contract.builder()
          .id(1L).type(ContractType.FIXED_TERM).startDate(startDate).endDate(endDate)
          .grossSalary(salary).employeeId(2L).build();
      ContractDto fixedTermDto = new ContractDto(
          1L, ContractType.FIXED_TERM, startDate, endDate, salary, 2L);
      ContractDto newContractDto = new ContractDto(null, request.type(), startDate, endDate, salary, 2L);
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.existsOverlapping(2L, startDate, endDate)).thenReturn(false);
      when(mapper.toEntity(newContractDto)).thenReturn(fixedTerm);
      when(repository.save(fixedTerm)).thenReturn(fixedTerm);
      when(mapper.toDto(fixedTerm)).thenReturn(fixedTermDto);

      ContractDto result = service.create(2L, request);
      System.out.println("create fixed-term result: " + result);

      assertThat(result).isEqualTo(fixedTermDto);
      verify(repository).save(fixedTerm);
    }

    @Test
    void createRejectsFixedTermWithoutEndDate() {
      ContractRequest request = new ContractRequest(ContractType.FIXED_TERM, startDate, null, salary);

      assertThatThrownBy(() -> service.create(2L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("End date is required");
    }

    @Test
    void createRejectsEndDateBeforeStartDate() {
      ContractRequest request = new ContractRequest(
          ContractType.INTERNSHIP, startDate, startDate.minusDays(1), salary);

      assertThatThrownBy(() -> service.create(2L, request))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("End date must not be before start date");
    }

    @Test
    void createRejectsMissingEmployee() {
      ContractRequest request = new ContractRequest(ContractType.PERMANENT, startDate, null, salary);
      when(employeeRepository.lockById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.create(99L, request))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Employee not found: 99");
    }

    @Test
    void createRejectsOverlappingContract() {
      ContractRequest request = new ContractRequest(ContractType.PERMANENT, startDate, null, salary);
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.existsOverlapping(2L, startDate, null)).thenReturn(true);

      assertThatThrownBy(() -> service.create(2L, request))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining("already has a contract covering this period");
    }
  }

  @Nested
  class EndOperations {
    @Test
    void endMarksActiveContractAsEnded() {
      LocalDate endDate = LocalDate.now(ZoneOffset.UTC).minusDays(1);
      ContractHistoryDto endedDto = new ContractHistoryDto(
          1L, ContractType.PERMANENT, startDate, endDate, salary, 2L, ContractStatus.ENDED);
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.findById(1L)).thenReturn(Optional.of(contract));
      when(mapper.status(eq(contract), any(LocalDate.class))).thenReturn(ContractStatus.ACTIVE);
      when(repository.save(contract)).thenReturn(contract);
      when(mapper.toHistoryDto(eq(contract), any(LocalDate.class))).thenReturn(endedDto);

      ContractHistoryDto result = service.end(2L, 1L, new EndContractRequest(endDate));
      System.out.println("end result: " + result);

      assertThat(result).isEqualTo(endedDto);
      assertThat(contract.isEnded()).isTrue();
      assertThat(contract.getEndDate()).isEqualTo(endDate);
      verify(employeeRepository).lockById(2L);
      verify(repository).save(contract);
    }

    @Test
    void endRejectsMissingEmployee() {
      when(employeeRepository.lockById(99L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.end(99L, 1L, new EndContractRequest(startDate)))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Employee not found: 99");
    }

    @Test
    void endRejectsContractNotOwnedByEmployee() {
      contract.setEmployeeId(3L);
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.findById(1L)).thenReturn(Optional.of(contract));

      assertThatThrownBy(() -> service.end(2L, 1L, new EndContractRequest(startDate)))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Contract not found: 1");
    }

    @Test
    void endRejectsNonActiveContract() {
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.findById(1L)).thenReturn(Optional.of(contract));
      when(mapper.status(eq(contract), any(LocalDate.class))).thenReturn(ContractStatus.SCHEDULED);

      assertThatThrownBy(() -> service.end(2L, 1L, new EndContractRequest(startDate)))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining("Only an active contract can be ended");
    }

    @Test
    void endRejectsEndDateBeforeStartDate() {
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.findById(1L)).thenReturn(Optional.of(contract));
      when(mapper.status(eq(contract), any(LocalDate.class))).thenReturn(ContractStatus.ACTIVE);

      assertThatThrownBy(() -> service.end(2L, 1L, new EndContractRequest(startDate.minusDays(1))))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("End date must be between the start date and today");
    }

    @Test
    void endRejectsEndDateInTheFuture() {
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.findById(1L)).thenReturn(Optional.of(contract));
      when(mapper.status(eq(contract), any(LocalDate.class))).thenReturn(ContractStatus.ACTIVE);

      assertThatThrownBy(() -> service.end(2L, 1L,
          new EndContractRequest(LocalDate.now(ZoneOffset.UTC).plusDays(1))))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("End date must be between the start date and today");
    }

    @Test
    void endRejectsExtendingOriginalEndDate() {
      LocalDate today = LocalDate.now(ZoneOffset.UTC);
      contract.setEndDate(today.minusDays(2));
      when(employeeRepository.lockById(2L)).thenReturn(Optional.of(2L));
      when(repository.findById(1L)).thenReturn(Optional.of(contract));
      when(mapper.status(eq(contract), any(LocalDate.class))).thenReturn(ContractStatus.ACTIVE);

      assertThatThrownBy(() -> service.end(2L, 1L, new EndContractRequest(today.minusDays(1))))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("cannot extend the contract");
    }
  }
}
