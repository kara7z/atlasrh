package com.example.atlasrh.repository;

import org.springframework.data.repository.ListCrudRepository;

import com.example.atlasrh.entity.Contract;

public interface ContractRepository extends ListCrudRepository<Contract, Long> {
}
