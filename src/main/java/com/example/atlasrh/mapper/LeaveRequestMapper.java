package com.example.atlasrh.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.atlasrh.dto.LeaveRequestDto;
import com.example.atlasrh.entity.LeaveRequest;

@Mapper(config = MappingConfig.class)
public interface LeaveRequestMapper {
  LeaveRequestDto toDto(LeaveRequest leaveRequest);

  @Mapping(target = "employee", ignore = true)
  LeaveRequest toEntity(LeaveRequestDto dto);
}
