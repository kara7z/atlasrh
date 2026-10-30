package com.example.atlasrh.controller;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Contracts", description = "Employee contract creation, history and ending")
@RequestMapping("/api/employees/{id}/contracts")
public class ContractController {
  private final ContractService service;

  public ContractController(ContractService service) {
    this.service = service;
  }

  @GetMapping
  @Operation(summary = "Get employee contract history", description = "Newest start date first, then ID descending. Status is SCHEDULED, ACTIVE or ENDED. Existing employee with no contracts returns [].")
  public List<ContractHistoryDto> history(
      @PathVariable @Positive(message = "Employee ID must be positive") Long id) {
    return service.history(id);
  }

  @PutMapping(value = "/{contractId}/end", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "End an active contract", description = "End date must be between start date and today (UTC), without extending the original period. Explicit ending is immediately ENDED. Only active contracts can be ended (409 otherwise).")
  public ContractHistoryDto end(
      @PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @PathVariable @Positive(message = "Contract ID must be positive") Long contractId,
      @Valid @RequestBody EndContractRequest request) {
    return service.end(id, contractId, request);
  }

  @PutMapping(value = "/{contractId}/end",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @Hidden
  public ContractHistoryDto endForm(
      @PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @PathVariable @Positive(message = "Contract ID must be positive") Long contractId,
      @Valid @ModelAttribute EndContractRequest request) {
    return service.end(id, contractId, request);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Create an employee contract", description = "FIXED_TERM and INTERNSHIP require endDate. End must be on/after start (400). Inclusive contract periods cannot overlap for an employee (409). PERMANENT may omit endDate.")
  public ResponseEntity<ContractDto> create(
      @PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @Valid @RequestBody ContractRequest request) {
    ContractDto contract = service.create(id, request);
    return ResponseEntity.created(URI.create("/api/employees/" + id + "/contracts/" + contract.id())).body(contract);
  }

  @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE})
  @Hidden
  public ResponseEntity<ContractDto> createForm(
      @PathVariable @Positive(message = "Employee ID must be positive") Long id,
      @Valid @ModelAttribute ContractRequest request) {
    return create(id, request);
  }
}
