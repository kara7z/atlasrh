package com.example.atlasrh.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.mapper.LeaveRequestMapper;
import com.example.atlasrh.repository.LeaveRequestRepository;
import com.example.atlasrh.service.LeaveRequestService;

@Service
@Transactional(readOnly = true)
public class LeaveRequestServiceImpl implements LeaveRequestService {
  private final LeaveRequestRepository repository;
  private final LeaveRequestMapper mapper;

  public LeaveRequestServiceImpl(LeaveRequestRepository repository, LeaveRequestMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<LeaveRequestDto> findAll() {
    return repository.findAll().stream().map(mapper::toDto).toList();
  }

  @Override
  public Optional<LeaveRequestDto> findById(Long id) {
    return repository.findById(id).map(mapper::toDto);
  }
}
