package com.demo.homeloan.service;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.Role;

public interface DashboardService {
  DashboardResponse get(Role role);
}
