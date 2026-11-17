package com.demo.homeloan.service.impl;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.*;
import com.demo.homeloan.exception.ApiException;
import com.demo.homeloan.repository.*;
import com.demo.homeloan.service.AuthService;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
  private final UserRepository users;
  private final CustomerRepository customers;
  private final PasswordEncoder encoder;

  private String dummyHash;

  @jakarta.annotation.PostConstruct
  void init() {
    dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
  }

  public static String email(String value) {
    return value.trim().toLowerCase(Locale.ROOT);
  }

  public static void checkPassword(String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw ApiException.bad("Password exceeds BCrypt's 72-byte limit");
  }

  public static void adult(LocalDate dob) {
    if (dob.isAfter(LocalDate.now(ZoneOffset.UTC).minusYears(18)))
      throw ApiException.bad("Customer must be at least 18 years old");
  }

  @Transactional
  public AuthResponse register(RegisterRequest r) {
    adult(r.dateOfBirth());
    checkPassword(r.password());
    String e = email(r.email());
    if (users.existsByEmail(e)) throw ApiException.conflict("Email already registered");
    User u = new User();
    u.setEmail(e);
    u.setFullName(r.fullName().trim());
    u.setPasswordHash(encoder.encode(r.password()));
    u.setRole(Role.CUSTOMER);
    users.save(u);
    Customer c = new Customer();
    c.setUser(u);
    c.setMobile(r.mobile());
    c.setDateOfBirth(r.dateOfBirth());
    c.setAddress(r.address().trim());
    c.setEmploymentType(r.employmentType());
    c.setMonthlyIncome(r.monthlyIncome());
    customers.save(c);
    return response(u);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest r) {
    checkPassword(r.password());
    User u = users.findByEmail(email(r.email())).orElse(null);
    boolean matches = encoder.matches(r.password(), u == null ? dummyHash : u.getPasswordHash());
    if (u == null || !matches || !u.isEnabled())
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    return response(u);
  }

  private AuthResponse response(User u) {
    return new AuthResponse("Basic", u.getId(), u.getFullName(), u.getEmail(), u.getRole());
  }
}
