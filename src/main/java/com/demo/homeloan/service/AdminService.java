package com.demo.homeloan.service;

import com.demo.homeloan.dto.ApiDtos.*;
import java.util.List;

public interface AdminService {
  PageResponse<CustomerResponse> customers(int page, int size);

  CustomerResponse updateCustomer(Long id, ProfileRequest r);

  CustomerResponse enableCustomer(Long id, boolean enabled);

  PageResponse<UserResponse> officers(int page, int size);

  UserResponse createOfficer(OfficerRequest r);

  UserResponse updateOfficer(Long id, OfficerUpdate r);

  List<ProductResponse> products();
}
