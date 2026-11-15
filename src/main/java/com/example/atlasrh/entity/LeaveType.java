package com.example.atlasrh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveType {
  @Id
  @Column(nullable = false)
  private String code;

  @Column(name = "days_per_year", nullable = false)
  private int daysPerYear;
}
