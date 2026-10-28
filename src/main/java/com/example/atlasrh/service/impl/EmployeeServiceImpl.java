package com.example.atlasrh.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.dto.EmployeeRequest;
import com.example.atlasrh.entity.Employee;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.mapper.EmployeeMapper;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.service.EmployeeService;

@Service
@Transactional(readOnly = true)
public class EmployeeServiceImpl implements EmployeeService {
  private final EmployeeRepository repository;
  private final EmployeeMapper mapper;
  private final DepartmentRepository departmentRepository;

  public EmployeeServiceImpl(EmployeeRepository repository, EmployeeMapper mapper,
      DepartmentRepository departmentRepository) {
    this.repository = repository;
    this.mapper = mapper;
    this.departmentRepository = departmentRepository;
  }

  @Override
  public List<EmployeeDto> findAll() {
    return repository.findAll().stream().map(mapper::toDto).toList();
  }

  @Override
  public Optional<EmployeeDto> findById(Long id) {
    return repository.findById(id).map(mapper::toDto);
  }

  @Override
  public List<EmployeeDto> findTeam(Long managerId) {
    if (!repository.existsById(managerId)) {
      throw new ResourceNotFoundException("Manager not found: " + managerId);
    }
    return repository.findByManagerId(managerId).stream().map(mapper::toDto).toList();
  }

  @Override
  @Transactional
  public EmployeeDto create(EmployeeRequest request) {
    if (repository.existsByEmployeeNumber(request.employeeNumber()) || repository.existsByEmail(request.email())) {
      throw duplicateEmployee();
    }
    validateReferences(null, request);
    return save(mapper.toEntity(toDto(null, request)));
  }

  @Override
  @Transactional
  public EmployeeDto update(Long id, EmployeeRequest request) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("Employee not found: " + id);
    }
    if (repository.existsByEmployeeNumberAndIdNot(request.employeeNumber(), id)
        || repository.existsByEmailAndIdNot(request.email(), id)) {
      throw duplicateEmployee();
    }
    validateReferences(id, request);
    return save(mapper.toEntity(toDto(id, request)));
  }

  private void validateReferences(Long id, EmployeeRequest request) {
    if (id != null && id.equals(request.managerId())) {
      throw new BadRequestException("An employee cannot be their own manager");
    }
    if (!departmentRepository.existsById(request.departmentId())) {
      throw new ResourceNotFoundException("Department not found: " + request.departmentId());
    }
    if (request.managerId() != null && !repository.existsById(request.managerId())) {
      throw new ResourceNotFoundException("Manager not found: " + request.managerId());
    }
  }

  private EmployeeDto toDto(Long id, EmployeeRequest request) {
    return new EmployeeDto(id, request.employeeNumber(), request.firstName(), request.lastName(), request.email(),
        request.position(), request.hireDate(), request.departmentId(), request.managerId());
  }

  private EmployeeDto save(Employee employee) {
    try {
      return mapper.toDto(repository.save(employee));
    } catch (DataIntegrityViolationException | DbActionExecutionException exception) {
      for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
        if (cause instanceof DuplicateKeyException) {
          throw duplicateEmployee();
        }
      }
      throw exception;
    }
  }

  private ConflictException duplicateEmployee() {
    return new ConflictException("An employee with this employee number or email already exists");
  }
}
