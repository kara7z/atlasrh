package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.atlasrh.dto.ContractRequest;
import com.example.atlasrh.dto.EndContractRequest;
import com.example.atlasrh.entity.Contract;
import com.example.atlasrh.enums.ContractType;
import com.example.atlasrh.enums.ContractStatus;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.mapper.ContractMapper;
import com.example.atlasrh.repository.ContractRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.service.impl.ContractServiceImpl;

@ExtendWith(MockitoExtension.class)
class ContractServiceTests {
  @Mock
  private ContractRepository repository;
  @Mock
  private EmployeeRepository employeeRepository;
  private ContractService service;
  private final LocalDate start = LocalDate.of(2026, 1, 1);

  @BeforeEach
  void setUp() {
    service = new ContractServiceImpl(repository, Mappers.getMapper(ContractMapper.class), employeeRepository);
  }

  @ParameterizedTest
  @EnumSource(ContractStatus.class)
  void historyMapsCurrentStatus(ContractStatus status) {
    var today = LocalDate.now(ZoneOffset.UTC);
    var contract = activeContract(today);
    if (status == ContractStatus.SCHEDULED) {
      contract.setStartDate(today.plusDays(1));
    } else if (status == ContractStatus.ENDED) {
      contract.setEndDate(today.minusDays(1));
    }
    when(employeeRepository.existsById(1L)).thenReturn(true);
    when(repository.findByEmployeeIdOrderByStartDateDescIdDesc(1L)).thenReturn(List.of(contract));
    var history = service.history(1L);
    assertThat(history).hasSize(1);
    assertThat(history.getFirst().status()).isEqualTo(status);
    assertThat(history.getFirst().employeeId()).isEqualTo(1L);
  }

  @Test
  void historyDistinguishesMissingEmployeeAndEmptyHistory() {
    assertThatThrownBy(() -> service.history(99L)).isInstanceOf(ResourceNotFoundException.class);
    when(employeeRepository.existsById(1L)).thenReturn(true);
    when(repository.findByEmployeeIdOrderByStartDateDescIdDesc(1L)).thenReturn(List.of());
    assertThat(service.history(1L)).isEmpty();
  }

  @Test
  void endsAnActiveContractTodayAndPreservesItsFields() {
    var today = LocalDate.now(ZoneOffset.UTC);
    var contract = activeContract(today);
    when(employeeRepository.lockById(1L)).thenReturn(Optional.of(1L));
    when(repository.findById(2L)).thenReturn(Optional.of(contract));
    when(repository.save(contract)).thenReturn(contract);
    var ended = service.end(1L, 2L, new EndContractRequest(today));
    assertThat(ended.status()).isEqualTo(ContractStatus.ENDED);
    assertThat(ended.endDate()).isEqualTo(today);
    assertThat(ended.id()).isEqualTo(2L);
    assertThat(ended.type()).isEqualTo(ContractType.PERMANENT);
    assertThat(contract.isEnded()).isTrue();
  }

  @Test
  void rejectsEndingMissingOrWrongEmployeeContracts() {
    var today = LocalDate.now(ZoneOffset.UTC);
    assertThatThrownBy(() -> service.end(99L, 2L, new EndContractRequest(today)))
        .isInstanceOf(ResourceNotFoundException.class);
    when(employeeRepository.lockById(1L)).thenReturn(Optional.of(1L));
    assertThatThrownBy(() -> service.end(1L, 2L, new EndContractRequest(today)))
        .isInstanceOf(ResourceNotFoundException.class);
    var contract = activeContract(today);
    contract.setEmployeeId(3L);
    when(repository.findById(2L)).thenReturn(Optional.of(contract));
    assertThatThrownBy(() -> service.end(1L, 2L, new EndContractRequest(today)))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsRepeatedEndingExpiredAndScheduledContracts() {
    var today = LocalDate.now(ZoneOffset.UTC);
    var contract = activeContract(today);
    when(employeeRepository.lockById(1L)).thenReturn(Optional.of(1L));
    when(repository.findById(2L)).thenReturn(Optional.of(contract));
    contract.setEnded(true);
    assertThatThrownBy(() -> service.end(1L, 2L, new EndContractRequest(today)))
        .isInstanceOf(ConflictException.class);
    contract.setEnded(false);
    contract.setEndDate(today.minusDays(1));
    assertThatThrownBy(() -> service.end(1L, 2L, new EndContractRequest(today)))
        .isInstanceOf(ConflictException.class);
    contract.setEndDate(null);
    contract.setStartDate(today.plusDays(1));
    assertThatThrownBy(() -> service.end(1L, 2L, new EndContractRequest(today)))
        .isInstanceOf(ConflictException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsEndDatesBeforeStartOrInTheFuture() {
    var today = LocalDate.now(ZoneOffset.UTC);
    var contract = activeContract(today);
    when(employeeRepository.lockById(1L)).thenReturn(Optional.of(1L));
    when(repository.findById(2L)).thenReturn(Optional.of(contract));
    assertThatThrownBy(() -> service.end(1L, 2L, new EndContractRequest(contract.getStartDate().minusDays(1))))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> service.end(1L, 2L, new EndContractRequest(today.plusDays(1))))
        .isInstanceOf(BadRequestException.class);
    verify(repository, never()).save(any());
  }

  private Contract activeContract(LocalDate today) {
    return Contract.builder().id(2L).employeeId(1L).type(ContractType.PERMANENT)
        .startDate(today.minusMonths(1)).grossSalary(new BigDecimal("12000.00")).build();
  }

  @ParameterizedTest
  @EnumSource(ContractType.class)
  void createsContractWithGeneratedIdAndEmployeeFromPath(ContractType type) {
    LocalDate end = type == ContractType.PERMANENT ? null : start.plusMonths(6);
    when(employeeRepository.lockById(1L)).thenReturn(Optional.of(1L));
    when(repository.save(any(Contract.class))).thenAnswer(invocation -> {
      Contract entity = invocation.getArgument(0);
      assertThat(entity.getId()).isNull();
      assertThat(entity.getEmployeeId()).isEqualTo(1L);
      assertThat(entity.getEmployee()).isNull();
      return Contract.builder().id(2L).employeeId(1L).type(entity.getType()).startDate(entity.getStartDate())
          .endDate(entity.getEndDate()).grossSalary(entity.getGrossSalary()).build();
    });
    var result = service.create(1L, request(type, end));
    assertThat(result.id()).isEqualTo(2L);
    assertThat(result.type()).isEqualTo(type);
    assertThat(result.endDate()).isEqualTo(end);
    assertThat(result.grossSalary()).isEqualByComparingTo("12000.00");
    verify(repository).existsOverlapping(1L, start, end);
  }

  @ParameterizedTest
  @EnumSource(value = ContractType.class, names = {"FIXED_TERM", "INTERNSHIP"})
  void requiresEndDateForNonPermanentContracts(ContractType type) {
    assertThatThrownBy(() -> service.create(1L, request(type, null))).isInstanceOf(BadRequestException.class);
    verify(employeeRepository, never()).lockById(any());
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsReversedDates() {
    assertThatThrownBy(() -> service.create(1L, request(ContractType.PERMANENT, start.minusDays(1))))
        .isInstanceOf(BadRequestException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void missingEmployeeReturns404() {
    assertThatThrownBy(() -> service.create(99L, request(ContractType.PERMANENT, null)))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(repository, never()).existsOverlapping(any(), any(), any());
    verify(repository, never()).save(any());
  }

  @Test
  void overlappingContractReturns409WithoutSaving() {
    when(employeeRepository.lockById(1L)).thenReturn(Optional.of(1L));
    when(repository.existsOverlapping(1L, start, null)).thenReturn(true);
    assertThatThrownBy(() -> service.create(1L, request(ContractType.PERMANENT, null)))
        .isInstanceOf(ConflictException.class);
    verify(repository, never()).save(any());
  }

  private ContractRequest request(ContractType type, LocalDate end) {
    return new ContractRequest(type, start, end, new BigDecimal("12000.00"));
  }
}
