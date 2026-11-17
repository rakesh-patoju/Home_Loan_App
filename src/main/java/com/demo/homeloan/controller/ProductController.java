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
@RequestMapping("/api/loan-products")
@RequiredArgsConstructor
public class ProductController {
  private final ProductService service;

  @GetMapping
  public List<ProductResponse> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  public ProductResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse create(@Valid @RequestBody ProductRequest r) {
    return service.create(r);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest r) {
    return service.update(id, r);
  }
}
