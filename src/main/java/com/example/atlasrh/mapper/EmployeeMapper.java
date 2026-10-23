package com.example.atlasrh.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.atlasrh.dto.EmployeeDto;
import com.example.atlasrh.entity.Employee;

@Mapper(config = MappingConfig.class)
public interface EmployeeMapper {
  EmployeeDto toDto(Employee employee);

  @Mapping(target = "department", ignore = true)
  @Mapping(target = "manager", ignore = true)
  @Mapping(target = "team", ignore = true)
  @Mapping(target = "contracts", ignore = true)
  @Mapping(target = "leaveRequests", ignore = true)
  Employee toEntity(EmployeeDto dto);
}
