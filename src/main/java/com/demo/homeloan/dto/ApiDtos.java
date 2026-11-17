package com.demo.homeloan.dto;

import com.demo.homeloan.entity.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class ApiDtos {
  private ApiDtos() {}

  public record RegisterRequest(
      @NotBlank @Size(max = 100) String fullName,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 10, max = 64) String password,
      @NotBlank @Pattern(regexp = "[0-9]{10,15}") String mobile,
      @NotNull @Past LocalDate dateOfBirth,
      @NotBlank @Size(max = 500) String address,
      @NotNull EmploymentType employmentType,
      @NotNull @DecimalMin("0") @DecimalMax("9999999999999.99") @Digits(integer = 13, fraction = 2)
          BigDecimal monthlyIncome) {}

  public record LoginRequest(
      @NotBlank @Email String email, @NotBlank @Size(max = 64) String password) {}

  public record AuthResponse(
      String authType,
      Long userId,
      String fullName,
      String email,
      Role role) {}

  public record ProfileRequest(
      @NotBlank @Size(max = 100) String fullName,
      @NotBlank @Pattern(regexp = "[0-9]{10,15}") String mobile,
      @NotNull @Past LocalDate dateOfBirth,
      @NotBlank @Size(max = 500) String address,
      @NotNull EmploymentType employmentType,
      @NotNull @DecimalMin("0") @DecimalMax("9999999999999.99") @Digits(integer = 13, fraction = 2)
          BigDecimal monthlyIncome) {}

  public record CustomerResponse(
      Long id,
      Long userId,
      String fullName,
      String email,
      String mobile,
      LocalDate dateOfBirth,
      String address,
      EmploymentType employmentType,
      BigDecimal monthlyIncome,
      boolean enabled) {}

  public record ProductRequest(
      @NotBlank @Size(max = 100) String productName,
      @NotNull @DecimalMin("1") @Digits(integer = 13, fraction = 2) BigDecimal minimumLoanAmount,
      @NotNull @DecimalMin("1") @Digits(integer = 13, fraction = 2) BigDecimal maximumLoanAmount,
      @NotNull @DecimalMin("0") @DecimalMax("30") @Digits(integer = 2, fraction = 2)
          BigDecimal interestRate,
      @Min(12) @Max(360) int minimumTenureMonths,
      @Min(12) @Max(360) int maximumTenureMonths,
      @NotBlank @Size(max = 1000) String description,
      boolean active) {}

  public record ProductResponse(
      Long id,
      String productName,
      BigDecimal minimumLoanAmount,
      BigDecimal maximumLoanAmount,
      BigDecimal interestRate,
      int minimumTenureMonths,
      int maximumTenureMonths,
      String description,
      boolean active) {}

  public record EligibilityRequest(
      @NotNull @DecimalMin("1") @DecimalMax("1000000000") BigDecimal monthlyIncome,
      @NotNull @DecimalMin("0") @DecimalMax("1000000000") BigDecimal existingEmi,
      @NotNull @DecimalMin("1") @DecimalMax("10000000000") BigDecimal requestedLoanAmount,
      @Min(12) @Max(360) int tenureMonths,
      @NotNull @DecimalMin("0") @DecimalMax("30") BigDecimal interestRate) {}

  public record EligibilityResponse(
      BigDecimal estimatedMonthlyEmi,
      BigDecimal debtToIncomePercent,
      BigDecimal maxDtiPercent,
      BigDecimal minimumMonthlyIncome,
      boolean eligible,
      String disclaimer) {}

  public record ApplicationRequest(
      @NotNull @Positive Long loanProductId,
      @NotNull @DecimalMin("1") @Digits(integer = 13, fraction = 2) BigDecimal requestedLoanAmount,
      @Min(12) @Max(360) int tenureMonths,
      @NotNull @DecimalMin("1") @Digits(integer = 13, fraction = 2) BigDecimal monthlyIncome,
      @NotNull EmploymentType employmentType,
      @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 2) BigDecimal existingEmi) {}

  public record StatusRequest(
      @NotNull ApplicationStatus status, @NotBlank @Size(max = 1000) String remarks) {}

  public record RemarkRequest(@NotBlank @Size(max = 1000) String remarks) {}

  public record AssignmentRequest(@NotNull @Positive Long officerId) {}

  public record ApplicationResponse(
      Long id,
      String referenceNumber,
      Long customerId,
      String customerName,
      Long loanProductId,
      String productName,
      Long assignedOfficerId,
      String assignedOfficerName,
      BigDecimal requestedLoanAmount,
      int tenureMonths,
      BigDecimal monthlyIncome,
      EmploymentType employmentType,
      BigDecimal existingEmi,
      BigDecimal interestRate,
      Instant applicationDate,
      ApplicationStatus status) {}

  public record DocumentResponse(
      Long id,
      DocumentType documentType,
      String originalFilename,
      String contentType,
      long sizeBytes,
      Instant uploadedAt,
      VerificationStatus verificationStatus,
      Long verifiedBy,
      Instant verifiedAt,
      String verificationRemarks) {}

  public record VerifyRequest(
      @NotNull VerificationStatus status, @NotBlank @Size(max = 1000) String remarks) {}

  public record HistoryResponse(
      Long id,
      ApplicationStatus fromStatus,
      ApplicationStatus toStatus,
      Long changedBy,
      String changedByName,
      String remarks,
      Instant changedAt) {}

  public record DashboardResponse(
      long totalCustomers,
      long totalApplications,
      long pendingApplications,
      long pendingVerification,
      long approvedApplications,
      long rejectedApplications,
      List<ApplicationResponse> recentApplications) {}

  public record OfficerRequest(
      @NotBlank @Size(max = 100) String fullName,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 10, max = 64) String password) {}

  public record OfficerUpdate(@NotBlank @Size(max = 100) String fullName, boolean enabled) {}

  public record EnabledRequest(boolean enabled) {}

  public record UserResponse(Long id, String fullName, String email, Role role, boolean enabled) {}

  public record PageResponse<T>(
      List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> p) {
      return new PageResponse<>(
          p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
  }
}
