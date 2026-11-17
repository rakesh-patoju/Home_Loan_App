package com.demo.homeloan.service;

import com.demo.homeloan.dto.ApiDtos.*;

public interface AuthService {
  AuthResponse register(RegisterRequest request);

  AuthResponse login(LoginRequest request);
}
