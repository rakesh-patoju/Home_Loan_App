package com.demo.homeloan.service.impl;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.*;
import com.demo.homeloan.exception.ApiException;
import com.demo.homeloan.mapper.ApiMapper;
import com.demo.homeloan.repository.CustomerRepository;
import com.demo.homeloan.security.CurrentUser;
import com.demo.homeloan.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {
  private final CustomerRepository customers;
  private final CurrentUser current;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public CustomerResponse profile() {
    return mapper.customer(
        customers
            .findByUserId(current.require(Role.CUSTOMER).getId())
            .orElseThrow(() -> ApiException.missing("Customer not found")));
  }

  @Transactional
  public CustomerResponse update(ProfileRequest r) {
    Customer c =
        customers
            .lockByUserId(current.require(Role.CUSTOMER).getId())
            .orElseThrow(() -> ApiException.missing("Customer not found"));
    AuthServiceImpl.adult(r.dateOfBirth());
    c.getUser().setFullName(r.fullName().trim());
    c.setMobile(r.mobile());
    c.setDateOfBirth(r.dateOfBirth());
    c.setAddress(r.address().trim());
    c.setEmploymentType(r.employmentType());
    c.setMonthlyIncome(r.monthlyIncome());
    return mapper.customer(c);
  }
}
