package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.ContractRequest;
import com.example.atlasrh.dto.EndContractRequest;
import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.enums.ContractType;
import com.example.atlasrh.enums.ContractStatus;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.repository.ContractRepository;

@SpringBootTest
@Transactional
class ContractIntegrationTests {
  @Autowired
  private ContractService service;
  @Autowired
  private EmployeeService employeeService;
  @Autowired
  private EmployeeRepository employeeRepository;
  @Autowired
  private DepartmentRepository departmentRepository;
  @Autowired
  private ContractRepository contractRepository;

  @Test
  void endingPersistsStateAndShortensIntervalForReplacement() {
    var today = LocalDate.now(ZoneOffset.UTC);
    Long employee = employee(department());
    var created = service.create(employee, request(today.minusMonths(1).toString(), null));
    var ended = service.end(employee, created.id(), new EndContractRequest(today));
    assertThat(ended.status()).isEqualTo(ContractStatus.ENDED);
    assertThat(ended.endDate()).isEqualTo(today);
    assertThat(contractRepository.findById(created.id()).orElseThrow().isEnded()).isTrue();
    assertThat(service.history(employee)).containsExactly(ended);
    assertThatThrownBy(() -> service.end(employee, created.id(), new EndContractRequest(today)))
        .isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> service.create(employee, request(today.toString(), null)))
        .isInstanceOf(ConflictException.class);
    var replacement = service.create(employee, request(today.plusDays(1).toString(), null));
    var history = service.history(employee);
    assertThat(history).hasSize(2);
    assertThat(history.getFirst().id()).isEqualTo(replacement.id());
    assertThat(history.getFirst().status()).isEqualTo(ContractStatus.SCHEDULED);
    assertThat(history.getLast()).isEqualTo(ended);
  }

  @Test
  void historyIncludesNaturalExpiryAndDistinguishesMissingAndEmptyEmployee() {
    var today = LocalDate.now(ZoneOffset.UTC);
    Long employee = employee(department());
    assertThat(service.history(employee)).isEmpty();
    service.create(employee, request(today.minusYears(1).toString(), today.minusDays(1).toString()));
    service.create(employee, request(today.toString(), null));
    assertThat(service.history(employee)).extracting(item -> item.status())
        .containsExactly(ContractStatus.ACTIVE, ContractStatus.ENDED);
    assertThatThrownBy(() -> service.history(Long.MAX_VALUE)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void cannotEndAnotherEmployeesContract() {
    var today = LocalDate.now(ZoneOffset.UTC);
    Long department = department();
    Long employee = employee(department);
    var created = service.create(employee, request(today.minusDays(1).toString(), null));
    Long other = employee(department);
    assertThatThrownBy(() -> service.end(other, created.id(), new EndContractRequest(today)))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThat(service.history(employee).getFirst().status()).isEqualTo(ContractStatus.ACTIVE);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void simultaneousEndRequestsYieldOneSuccessAndOneConflict() throws Exception {
    Long department = department();
    Long employee = employee(department);
    var today = LocalDate.now(ZoneOffset.UTC);
    var created = service.create(employee, request(today.minusDays(1).toString(), null));
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      CountDownLatch start = new CountDownLatch(1);
      Callable<String> end = () -> {
        if (!start.await(10, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Concurrent test did not start");
        }
        try {
          service.end(employee, created.id(), new EndContractRequest(today));
          return "ended";
        } catch (ConflictException exception) {
          return "conflict";
        }
      };
      var first = executor.submit(end);
      var second = executor.submit(end);
      start.countDown();
      assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder("ended", "conflict");
    } finally {
      contractRepository.deleteById(created.id());
      employeeRepository.deleteById(employee);
      departmentRepository.deleteById(department);
    }
  }

  @Test
  void createsAndReadsPermanentContract() {
    Long employeeId = employee(department());
    var created = service.create(employeeId, request("2026-01-01", null));
    assertThat(created.id()).isNotNull();
    assertThat(created.employeeId()).isEqualTo(employeeId);
    assertThat(service.findById(created.id())).contains(created);
  }

  @Test
  void permitsDisjointHistoryAndRejectsInclusiveOverlap() {
    Long employeeId = employee(department());
    service.create(employeeId, request("2025-01-01", "2025-12-31"));
    service.create(employeeId, request("2026-01-01", "2026-12-31"));
    assertThatThrownBy(() -> service.create(employeeId, request("2026-12-31", null)))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void openEndedContractBlocksOverlappingFutureContractButNotAnotherEmployee() {
    Long department = department();
    Long employee = employee(department);
    service.create(employee, request("2026-01-01", null));
    assertThatThrownBy(() -> service.create(employee, request("2027-01-01", null)))
        .isInstanceOf(ConflictException.class);
    assertThat(service.create(employee(department), request("2027-01-01", null)).id()).isNotNull();
  }

  @Test
  void missingEmployeeReturns404() {
    assertThatThrownBy(() -> service.create(Long.MAX_VALUE, request("2026-01-01", null)))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void concurrentOverlappingCreatesYieldOneSuccessAndOneConflict() throws Exception {
    Long department = department();
    Long employee = employee(department);
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      CountDownLatch start = new CountDownLatch(1);
      Callable<String> create = () -> {
        if (!start.await(10, TimeUnit.SECONDS)) {
          throw new IllegalStateException("Concurrent test did not start");
        }
        try {
          service.create(employee, request("2026-01-01", null));
          return "created";
        } catch (ConflictException exception) {
          return "conflict";
        }
      };
      var first = executor.submit(create);
      var second = executor.submit(create);
      start.countDown();
      assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder("created", "conflict");
    } finally {
      // This test deliberately commits to exercise real concurrent transactions.
      contractRepository.findAll().stream().filter(contract -> employee.equals(contract.getEmployeeId()))
          .forEach(contractRepository::delete);
      employeeRepository.deleteById(employee);
      departmentRepository.deleteById(department);
    }
  }

  private Long department() {
    return departmentRepository.save(Department.builder().name("ATLAS-16-" + UUID.randomUUID()).build()).getId();
  }

  private Long employee(Long department) {
    String number = UUID.randomUUID().toString();
    return employeeService.create(new EmployeeRequest(number, "Sara", "Amrani", number + "@example.com",
        "Developer", LocalDate.of(2025, 1, 1), department, null)).id();
  }

  private ContractRequest request(String start, String end) {
    return new ContractRequest(end == null ? ContractType.PERMANENT : ContractType.FIXED_TERM,
        LocalDate.parse(start), end == null ? null : LocalDate.parse(end), new BigDecimal("12000.00"));
  }
}
