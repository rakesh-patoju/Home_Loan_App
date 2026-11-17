package com.demo.homeloan.controller;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.*;
import com.demo.homeloan.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
  private final DashboardService service;

  @GetMapping("/customer")
  @PreAuthorize("hasRole('CUSTOMER')")
  public DashboardResponse customer() {
    return service.get(Role.CUSTOMER);
  }

  @GetMapping("/officer")
  @PreAuthorize("hasRole('LOAN_OFFICER')")
  public DashboardResponse officer() {
    return service.get(Role.LOAN_OFFICER);
  }

  @GetMapping("/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public DashboardResponse admin() {
    return service.get(Role.ADMIN);
  }
}
