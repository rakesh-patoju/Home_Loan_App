package com.demo.homeloan.service.impl;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.*;
import com.demo.homeloan.exception.ApiException;
import com.demo.homeloan.mapper.ApiMapper;
import com.demo.homeloan.repository.*;
import com.demo.homeloan.service.AdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
  private final CustomerRepository customers;
  private final UserRepository users;
  private final LoanApplicationRepository applications;
  private final LoanProductRepository products;
  private final PasswordEncoder encoder;
  private final ApiMapper mapper;

  private Pageable page(int p, int s) {
    if (p < 0 || s < 1 || s > 100) throw ApiException.bad("page must be >=0 and size 1..100");
    return PageRequest.of(p, s, Sort.by("id"));
  }

  @Transactional(readOnly = true)
  public PageResponse<CustomerResponse> customers(int p, int s) {
    return PageResponse.of(customers.findAll(page(p, s)).map(mapper::customer));
  }

  @Transactional
  public CustomerResponse updateCustomer(Long id, ProfileRequest r) {
    Customer c =
        customers.findById(id).orElseThrow(() -> ApiException.missing("Customer not found"));
    AuthServiceImpl.adult(r.dateOfBirth());
    c.getUser().setFullName(r.fullName().trim());
    c.setMobile(r.mobile());
    c.setDateOfBirth(r.dateOfBirth());
    c.setAddress(r.address().trim());
    c.setEmploymentType(r.employmentType());
    c.setMonthlyIncome(r.monthlyIncome());
    return mapper.customer(c);
  }

  @Transactional
  public CustomerResponse enableCustomer(Long id, boolean enabled) {
    Customer c =
        customers.findById(id).orElseThrow(() -> ApiException.missing("Customer not found"));
    c.getUser().setEnabled(enabled);
    return mapper.customer(c);
  }

  @Transactional(readOnly = true)
  public PageResponse<UserResponse> officers(int p, int s) {
    return PageResponse.of(users.findByRole(Role.LOAN_OFFICER, page(p, s)).map(mapper::user));
  }

  @Transactional
  public UserResponse createOfficer(OfficerRequest r) {
    String email = AuthServiceImpl.email(r.email());
    if (users.existsByEmail(email)) throw ApiException.conflict("Email already registered");
    AuthServiceImpl.checkPassword(r.password());
    User u = new User();
    u.setFullName(r.fullName().trim());
    u.setEmail(email);
    u.setPasswordHash(encoder.encode(r.password()));
    u.setRole(Role.LOAN_OFFICER);
    return mapper.user(users.save(u));
  }

  @Transactional
  public UserResponse updateOfficer(Long id, OfficerUpdate r) {
    User u =
        users
            .findById(id)
            .filter(x -> x.getRole() == Role.LOAN_OFFICER)
            .orElseThrow(() -> ApiException.missing("Officer not found"));
    if (!r.enabled()
        && applications.countByAssignedOfficerIdAndStatusNotIn(id, ApplicationServiceImpl.CLOSED)
            > 0)
      throw ApiException.conflict("Reassign open applications before disabling this officer");
    u.setFullName(r.fullName().trim());
    u.setEnabled(r.enabled());
    return mapper.user(u);
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> products() {
    return products.findAll(Sort.by("id")).stream().map(mapper::product).toList();
  }
}
