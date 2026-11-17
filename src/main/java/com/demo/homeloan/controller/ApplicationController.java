package com.demo.homeloan.controller;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.*;
import com.demo.homeloan.service.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class ApplicationController {
  private final ApplicationService service;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('CUSTOMER')")
  public ApplicationResponse submit(
      @Valid @RequestBody ApplicationRequest r, @RequestHeader("Idempotency-Key") String key) {
    return service.submit(r, key);
  }

  @GetMapping("/my-applications")
  @PreAuthorize("hasRole('CUSTOMER')")
  public PageResponse<ApplicationResponse> mine(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "applicationDate,desc") String sort) {
    return service.list(true, page, size, sort);
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
  public PageResponse<ApplicationResponse> list(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "applicationDate,desc") String sort) {
    return service.list(false, page, size, sort);
  }

  @GetMapping("/{id}")
  public ApplicationResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @GetMapping("/{id}/customer")
  @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
  public CustomerResponse customer(@PathVariable Long id) {
    return service.customer(id);
  }

  @GetMapping("/{id}/history")
  public List<HistoryResponse> history(@PathVariable Long id) {
    return service.history(id);
  }

  @PutMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
  public ApplicationResponse status(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
    return service.status(id, r);
  }

  @PutMapping("/{id}/assignment")
  @PreAuthorize("hasRole('ADMIN')")
  public ApplicationResponse assign(
      @PathVariable Long id, @Valid @RequestBody AssignmentRequest r) {
    return service.assign(id, r);
  }

  @PostMapping("/{id}/remarks")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
  public HistoryResponse remark(@PathVariable Long id, @Valid @RequestBody RemarkRequest r) {
    return service.remark(id, r);
  }
}
