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
@RequestMapping("/api/eligibility")
@RequiredArgsConstructor
public class EligibilityController {
  private final EligibilityService service;

  @PostMapping("/check")
  @PreAuthorize("hasRole('CUSTOMER')")
  public EligibilityResponse check(@Valid @RequestBody EligibilityRequest r) {
    return service.check(r);
  }
}
