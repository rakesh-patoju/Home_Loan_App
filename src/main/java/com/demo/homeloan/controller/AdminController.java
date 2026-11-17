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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
  private final AdminService service;

  @GetMapping("/customers")
  public PageResponse<CustomerResponse> customers(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.customers(page, size);
  }

  @PutMapping("/customers/{id}")
  public CustomerResponse customer(@PathVariable Long id, @Valid @RequestBody ProfileRequest r) {
    return service.updateCustomer(id, r);
  }

  @PutMapping("/customers/{id}/enabled")
  public CustomerResponse enable(@PathVariable Long id, @RequestBody EnabledRequest r) {
    return service.enableCustomer(id, r.enabled());
  }

  @GetMapping("/loan-officers")
  public PageResponse<UserResponse> officers(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.officers(page, size);
  }

  @PostMapping("/loan-officers")
  @ResponseStatus(HttpStatus.CREATED)
  public UserResponse create(@Valid @RequestBody OfficerRequest r) {
    return service.createOfficer(r);
  }

  @PutMapping("/loan-officers/{id}")
  public UserResponse update(@PathVariable Long id, @Valid @RequestBody OfficerUpdate r) {
    return service.updateOfficer(id, r);
  }

  @GetMapping("/loan-products")
  public List<ProductResponse> products() {
    return service.products();
  }
}
