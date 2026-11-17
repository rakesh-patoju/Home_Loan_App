package com.demo.homeloan.controller;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.*;
import com.demo.homeloan.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {
  private final CustomerService service;

  @GetMapping("/profile")
  public CustomerResponse profile() {
    return service.profile();
  }

  @PutMapping("/profile")
  public CustomerResponse update(@Valid @RequestBody ProfileRequest r) {
    return service.update(r);
  }
}
