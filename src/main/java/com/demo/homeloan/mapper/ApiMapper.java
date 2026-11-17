package com.demo.homeloan.mapper;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.*;
import org.springframework.stereotype.Component;

@Component
public class ApiMapper {
  public CustomerResponse customer(Customer c) {
    User u = c.getUser();
    return new CustomerResponse(
        c.getId(),
        u.getId(),
        u.getFullName(),
        u.getEmail(),
        c.getMobile(),
        c.getDateOfBirth(),
        c.getAddress(),
        c.getEmploymentType(),
        c.getMonthlyIncome(),
        u.isEnabled());
  }

  public ProductResponse product(LoanProduct p) {
    return new ProductResponse(
        p.getId(),
        p.getProductName(),
        p.getMinimumLoanAmount(),
        p.getMaximumLoanAmount(),
        p.getInterestRate(),
        p.getMinimumTenureMonths(),
        p.getMaximumTenureMonths(),
        p.getDescription(),
        p.isActive());
  }

  public ApplicationResponse application(LoanApplication a) {
    User o = a.getAssignedOfficer();
    return new ApplicationResponse(
        a.getId(),
        a.getReferenceNumber(),
        a.getCustomer().getId(),
        a.getCustomer().getUser().getFullName(),
        a.getLoanProduct().getId(),
        a.getLoanProduct().getProductName(),
        o == null ? null : o.getId(),
        o == null ? null : o.getFullName(),
        a.getRequestedLoanAmount(),
        a.getTenureMonths(),
        a.getMonthlyIncome(),
        a.getEmploymentType(),
        a.getExistingEmi(),
        a.getInterestRate(),
        a.getApplicationDate(),
        a.getStatus());
  }

  public DocumentResponse document(LoanDocument d) {
    return new DocumentResponse(
        d.getId(),
        d.getDocumentType(),
        d.getOriginalFilename(),
        d.getContentType(),
        d.getSizeBytes(),
        d.getUploadedAt(),
        d.getVerificationStatus(),
        d.getVerifiedBy() == null ? null : d.getVerifiedBy().getId(),
        d.getVerifiedAt(),
        d.getVerificationRemarks());
  }

  public HistoryResponse history(ApplicationStatusHistory h) {
    return new HistoryResponse(
        h.getId(),
        h.getFromStatus(),
        h.getToStatus(),
        h.getChangedBy().getId(),
        h.getChangedBy().getFullName(),
        h.getRemarks(),
        h.getChangedAt());
  }

  public UserResponse user(User u) {
    return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getRole(), u.isEnabled());
  }
}
