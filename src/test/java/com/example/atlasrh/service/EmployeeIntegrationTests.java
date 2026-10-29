package com.example.atlasrh.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.repository.DepartmentRepository;

@SpringBootTest
@Transactional
class EmployeeIntegrationTests {
  @Autowired
  private EmployeeService service;
  @Autowired
  private DepartmentRepository departmentRepository;

  @Test
  void filtersByNameAndDepartmentWithCorrectGroupingAndPagination() {
    Long department = departmentId();
    var first = service.create(namedRequest(department, "Sara", "Alpha"));
    var second = service.create(namedRequest(department, "Omar", "Sara"));
    service.create(namedRequest(department, "Unrelated", "Person"));
    service.create(namedRequest(departmentId(), "Sara", "Elsewhere"));

    var page = service.search("  sArA  ", department, PageRequest.of(0, 1, Sort.by("lastName")));
    assertThat(page.getContent()).containsExactly(first);
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getTotalPages()).isEqualTo(2);
    assertThat(service.search("sara", department, PageRequest.of(1, 1, Sort.by("lastName"))).getContent())
        .containsExactly(second);
    var beyond = service.search("sara", department, PageRequest.of(5, 1));
    assertThat(beyond.getContent()).isEmpty();
    assertThat(beyond.getTotalElements()).isEqualTo(2);
  }

  @Test
  void supportsDepartmentOnlyBlankNameAndMissingDepartment() {
    Long department = departmentId();
    var first = service.create(namedRequest(department, "Same", "Name"));
    var second = service.create(namedRequest(department, "Same", "Name"));
    var pageable = PageRequest.of(0, 20);

    assertThat(service.search(null, department, pageable).getContent()).containsExactly(first, second);
    assertThat(service.search("  ", department, pageable).getContent()).containsExactly(first, second);
    assertThat(service.search(null, Long.MAX_VALUE, pageable).getContent()).isEmpty();
    assertThat(service.search(null, department, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "id")))
        .getContent()).containsExactly(second, first);
  }

  @Test
  void searchesWithoutDepartmentAndTreatsWildcardsAndQuotesAsLiteralInput() {
    String unique = UUID.randomUUID().toString();
    var first = service.create(namedRequest(departmentId(), unique + "%_", "O'Neil"));
    var second = service.create(namedRequest(departmentId(), unique + "Other", "Person"));
    var pageable = PageRequest.of(0, 20);

    assertThat(service.search(unique, null, pageable).getContent()).containsExactly(first, second);
    assertThat(service.search(unique + "%_", null, pageable).getContent()).containsExactly(first);
    assertThat(service.search("' OR 1=1 --", null, pageable).getContent()).isEmpty();
    assertThat(service.search("no-match-" + unique, null, pageable).getTotalElements()).isZero();
    assertThat(service.search(null, null, pageable).getTotalElements()).isGreaterThanOrEqualTo(2);
  }

  private EmployeeRequest namedRequest(Long departmentId, String firstName, String lastName) {
    var request = request(departmentId, null);
    return new EmployeeRequest(request.employeeNumber(), firstName, lastName, request.email(), request.position(),
        request.hireDate(), departmentId, null);
  }

  @Test
  void teamContainsOnlyDirectReportsAcrossDepartments() {
    Long departmentId = departmentId();
    var manager = service.create(request(departmentId, null));
    var first = service.create(request(departmentId, manager.id()));
    var second = service.create(request(departmentId(), manager.id()));
    service.create(request(departmentId, first.id()));
    service.create(request(departmentId, null));

    assertThat(service.findTeam(manager.id())).containsExactlyInAnyOrder(first, second);
    assertThat(service.findTeam(second.id())).isEmpty();
  }

  @Test
  void teamChangesWhenAnEmployeeIsReassignedOrManagerIsCleared() {
    Long departmentId = departmentId();
    var manager = service.create(request(departmentId, null));
    var otherManager = service.create(request(departmentId, null));
    var employee = service.create(request(departmentId, manager.id()));
    var reassigned = service.update(employee.id(), request(departmentId, otherManager.id()));

    assertThat(service.findTeam(manager.id())).isEmpty();
    assertThat(service.findTeam(otherManager.id())).containsExactly(reassigned);

    service.update(employee.id(), request(departmentId, null));
    assertThat(service.findTeam(otherManager.id())).isEmpty();
  }

  @Test
  void missingManagerDoesNotReturnAnEmptyTeam() {
    assertThatThrownBy(() -> service.findTeam(Long.MAX_VALUE)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void persistsCreatesAndUpdatesWithOptionalManager() {
    Long departmentId = departmentId();
    var manager = service.create(request(departmentId, null));
    var request = request(departmentId, manager.id());
    var employee = service.create(request);
    assertThat(employee.id()).isNotNull();
    assertThat(service.findById(employee.id())).contains(employee);

    var updated = service.update(employee.id(), new EmployeeRequest(request.employeeNumber(), "Updated",
        request.lastName(), request.email(), "Lead", request.hireDate(), departmentId, null));
    assertThat(updated.id()).isEqualTo(employee.id());
    assertThat(updated.firstName()).isEqualTo("Updated");
    assertThat(updated.managerId()).isNull();
    assertThat(service.findById(employee.id())).contains(updated);
  }

  @Test
  void rejectsDuplicateNumbersAndEmailsAndSelfManagement() {
    Long departmentId = departmentId();
    var first = service.create(request(departmentId, null));
    var second = service.create(request(departmentId, null));
    var duplicateNumber = new EmployeeRequest(first.employeeNumber(), "Sara", "Amrani", second.email(),
        "Developer", first.hireDate(), departmentId, null);
    var duplicateEmail = new EmployeeRequest(second.employeeNumber(), "Sara", "Amrani", first.email(),
        "Developer", first.hireDate(), departmentId, null);

    assertThatThrownBy(() -> service.create(duplicateNumber)).isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> service.create(duplicateEmail)).isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> service.update(second.id(), duplicateNumber)).isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> service.update(second.id(), duplicateEmail)).isInstanceOf(ConflictException.class);
    assertThatThrownBy(() -> service.update(second.id(), request(departmentId, second.id())))
        .isInstanceOf(BadRequestException.class);
  }

  private Long departmentId() {
    return departmentRepository.save(Department.builder().name("ATLAS-13-" + UUID.randomUUID()).build()).getId();
  }

  private EmployeeRequest request(Long departmentId, Long managerId) {
    String number = UUID.randomUUID().toString();
    return new EmployeeRequest(number, "Sara", "Amrani", number + "@example.com", "Developer",
        LocalDate.of(2026, 1, 1), departmentId, managerId);
  }
}
