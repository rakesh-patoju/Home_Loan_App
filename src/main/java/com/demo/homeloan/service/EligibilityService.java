package com.demo.homeloan.service;

import com.demo.homeloan.dto.ApiDtos.*;

public interface EligibilityService {
  EligibilityResponse check(EligibilityRequest request);
}
