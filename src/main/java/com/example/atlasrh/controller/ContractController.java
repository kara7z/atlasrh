package com.example.atlasrh.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.atlasrh.dto.ContractDto;
import com.example.atlasrh.dto.ContractRequest;
import com.example.atlasrh.dto.ContractHistoryDto;
import com.example.atlasrh.dto.EndContractRequest;
import com.example.atlasrh.service.ContractService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/employees/{id}/contracts")
public class ContractController {
  private final ContractService service;

  public ContractController(ContractService service) {
    this.service = service;
  }

  @GetMapping
  public List<ContractHistoryDto> history(
      @PathVariable @Positive(message = "Employee ID must be positive") Long id) {
    return service.history(id);
  }

  @PutMapping(value = "/{contractId}/end", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ContractHistoryDto end(
      @PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @PathVariable @Positive(message = "Contract ID must be positive") Long contractId,
      @Valid @RequestBody EndContractRequest request) {
    return service.end(id, contractId, request);
  }

  @PutMapping(value = "/{contractId}/end",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  public ContractHistoryDto endForm(
      @PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @PathVariable @Positive(message = "Contract ID must be positive") Long contractId,
      @Valid @ModelAttribute EndContractRequest request) {
    return service.end(id, contractId, request);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ContractDto> create(
      @PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @Valid @RequestBody ContractRequest request) {
    ContractDto contract = service.create(id, request);
    return ResponseEntity.created(URI.create("/api/employees/" + id + "/contracts/" + contract.id())).body(contract);
  }

  @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  public ResponseEntity<ContractDto> createForm(
      @PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @Valid @ModelAttribute ContractRequest request) {
    return create(id, request);
  }
}
