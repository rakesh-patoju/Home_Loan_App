package com.demo.homeloan.service;

import com.demo.homeloan.dto.ApiDtos.*;

public interface CustomerService {
  CustomerResponse profile();

  CustomerResponse update(ProfileRequest request);
}
