package com.example.atlasrh.service;

import java.util.List;
import java.util.Optional;

import com.example.atlasrh.dto.LeaveRequestDto;

public interface LeaveRequestService {
  List<LeaveRequestDto> findAll();

  Optional<LeaveRequestDto> findById(Long id);
}
