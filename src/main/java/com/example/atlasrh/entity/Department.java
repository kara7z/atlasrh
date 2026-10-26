package com.example.atlasrh.entity;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Table("departments")
public class Department {
  @Id
  @Setter(AccessLevel.NONE)
  private Long id;
  private String name;
  private String description;

  @Transient
  private List<Employee> employees = new ArrayList<>();

}
