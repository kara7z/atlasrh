package com.example.atlasrh.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.DepartmentDto;
import com.example.atlasrh.dto.DepartmentRequest;
import com.example.atlasrh.entity.Department;
import com.example.atlasrh.exception.DepartmentConflictException;
import com.example.atlasrh.exception.DepartmentNotFoundException;
import com.example.atlasrh.mapper.DepartmentMapper;
import com.example.atlasrh.repository.DepartmentRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.service.DepartmentService;

@Service
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {
  private final DepartmentRepository repository;
  private final DepartmentMapper mapper;
  private final EmployeeRepository employeeRepository;

  public DepartmentServiceImpl(DepartmentRepository repository, DepartmentMapper mapper,
      EmployeeRepository employeeRepository) {
    this.repository = repository;
    this.mapper = mapper;
    this.employeeRepository = employeeRepository;
  }

  @Override
  public List<DepartmentDto> findAll() {
    return repository.findAll().stream().map(mapper::toDto).toList();
  }

  @Override
  public Optional<DepartmentDto> findById(Long id) {
    return repository.findById(id).map(mapper::toDto);
  }

  @Override
  @Transactional
  public DepartmentDto create(DepartmentRequest request) {
    if (repository.existsByName(request.name())) {
      throw duplicateName();
    }
    Department department = mapper.toEntity(new DepartmentDto(null, request.name(), request.description()));
    return save(department);
  }

  @Override
  @Transactional
  public DepartmentDto update(Long id, DepartmentRequest request) {
    Department department = repository.findById(id)
        .orElseThrow(() -> new DepartmentNotFoundException(id));
    if (repository.existsByNameAndIdNot(request.name(), id)) {
      throw duplicateName();
    }
    department.setName(request.name());
    department.setDescription(request.description());
    return save(department);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Department department = repository.findById(id)
        .orElseThrow(() -> new DepartmentNotFoundException(id));
    if (employeeRepository.existsByDepartmentId(id)) {
      throw occupiedDepartment();
    }
    try {
      repository.delete(department);
    } catch (DataIntegrityViolationException | DbActionExecutionException exception) {
      if (hasCause(exception, DataIntegrityViolationException.class)) {
        throw occupiedDepartment();
      }
      throw exception;
    }
  }

  private DepartmentDto save(Department department) {
    try {
      return mapper.toDto(repository.save(department));
    } catch (DataIntegrityViolationException | DbActionExecutionException exception) {
      if (hasCause(exception, DuplicateKeyException.class)) {
        throw duplicateName();
      }
      throw exception;
    }
  }

  private boolean hasCause(Throwable exception, Class<? extends Throwable> type) {
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (type.isInstance(cause)) {
        return true;
      }
    }
    return false;
  }

  private DepartmentConflictException duplicateName() {
    return new DepartmentConflictException("A department with this name already exists");
  }

  private DepartmentConflictException occupiedDepartment() {
    return new DepartmentConflictException("Cannot delete a department with employees");
  }
}
