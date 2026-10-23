package com.example.atlasrh.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.atlasrh.dto.DepartmentDto;
import com.example.atlasrh.entity.Department;

@Mapper(config = MappingConfig.class)
public interface DepartmentMapper {
  DepartmentDto toDto(Department department);

  @Mapping(target = "employees", ignore = true)
  Department toEntity(DepartmentDto dto);
}
