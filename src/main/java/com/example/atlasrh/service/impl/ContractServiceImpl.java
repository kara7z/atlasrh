package com.example.atlasrh.service.impl;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.ContractDto;
import com.example.atlasrh.dto.ContractRequest;
import com.example.atlasrh.dto.ContractHistoryDto;
import com.example.atlasrh.dto.EndContractRequest;
import com.example.atlasrh.enums.ContractStatus;
import com.example.atlasrh.enums.ContractType;
import com.example.atlasrh.exception.BadRequestException;
import com.example.atlasrh.exception.ConflictException;
import com.example.atlasrh.exception.ResourceNotFoundException;
import com.example.atlasrh.mapper.ContractMapper;
import com.example.atlasrh.repository.ContractRepository;
import com.example.atlasrh.repository.EmployeeRepository;
import com.example.atlasrh.service.ContractService;

@Service
@Transactional(readOnly = true)
public class ContractServiceImpl implements ContractService {
  private final ContractRepository repository;
  private final ContractMapper mapper;
  private final EmployeeRepository employeeRepository;

  public ContractServiceImpl(ContractRepository repository, ContractMapper mapper,
      EmployeeRepository employeeRepository) {
    this.repository = repository;
    this.mapper = mapper;
    this.employeeRepository = employeeRepository;
  }

  @Override
  public List<ContractDto> findAll() {
    return repository.findAll().stream().map(mapper::toDto).toList();
  }

  @Override
  public Optional<ContractDto> findById(Long id) {
    return repository.findById(id).map(mapper::toDto);
  }

  @Override
  public List<ContractHistoryDto> history(Long employeeId) {
    if (!employeeRepository.existsById(employeeId)) {
      throw new ResourceNotFoundException("Employee not found: " + employeeId);
    }
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    return repository.findByEmployeeIdOrderByStartDateDescIdDesc(employeeId).stream()
        .map(contract -> mapper.toHistoryDto(contract, today)).toList();
  }

  @Override
  @Transactional
  public ContractHistoryDto end(Long employeeId, Long contractId, EndContractRequest request) {
    // Same lock as creation: contract changes for an employee cannot race.
    employeeRepository.lockById(employeeId)
        .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
    var contract = repository.findById(contractId)
        .filter(value -> employeeId.equals(value.getEmployeeId()))
        .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    if (mapper.status(contract, today) != ContractStatus.ACTIVE) {
      throw new ConflictException("Only an active contract can be ended");
    }
    if (request.endDate().isBefore(contract.getStartDate()) || request.endDate().isAfter(today)
        || (contract.getEndDate() != null && request.endDate().isAfter(contract.getEndDate()))) {
      throw new BadRequestException("End date must be between the start date and today and cannot extend the contract");
    }
    contract.setEndDate(request.endDate());
    contract.setEnded(true);
    return mapper.toHistoryDto(repository.save(contract), today);
  }

  @Override
  @Transactional
  public ContractDto create(Long employeeId, ContractRequest request) {
    if (request.type() != ContractType.PERMANENT && request.endDate() == null) {
      throw new BadRequestException("End date is required for fixed-term and internship contracts");
    }
    if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
      throw new BadRequestException("End date must not be before start date");
    }
    // Serialize contract creation per employee, including the first contract.
    employeeRepository.lockById(employeeId)
        .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
    if (repository.existsOverlapping(employeeId, request.startDate(), request.endDate())) {
      throw new ConflictException("Employee already has a contract covering this period");
    }
    ContractDto dto = new ContractDto(null, request.type(), request.startDate(), request.endDate(),
        request.grossSalary(), employeeId);
    return mapper.toDto(repository.save(mapper.toEntity(dto)));
  }
}
