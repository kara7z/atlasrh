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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.atlasrh.dto.ContractRequest;
import com.example.atlasrh.entity.Contract;
import com.example.atlasrh.enums.ContractType;
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
