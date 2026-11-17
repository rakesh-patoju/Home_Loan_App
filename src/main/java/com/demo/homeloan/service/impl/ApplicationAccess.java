package com.demo.homeloan.service.impl;

import com.demo.homeloan.entity.*;
import com.demo.homeloan.exception.ApiException;
import com.demo.homeloan.repository.LoanApplicationRepository;
import com.demo.homeloan.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationAccess {
  private final LoanApplicationRepository applications;
  private final CurrentUser current;

  public LoanApplication read(Long id) {
    LoanApplication a =
        applications.findById(id).orElseThrow(() -> ApiException.missing("Application not found"));
    authorize(a);
    return a;
  }

  public LoanApplication lock(Long id) {
    LoanApplication a =
        applications.lockById(id).orElseThrow(() -> ApiException.missing("Application not found"));
    authorize(a);
    return a;
  }

  public void authorize(LoanApplication a) {
    User u = current.get();
    boolean allowed =
        switch (u.getRole()) {
          case ADMIN -> true;
          case CUSTOMER -> a.getCustomer().getUser().getId().equals(u.getId());
          case LOAN_OFFICER ->
              a.getAssignedOfficer() != null && a.getAssignedOfficer().getId().equals(u.getId());
        };
    if (!allowed) throw ApiException.forbidden();
  }

  public void staff() {
    current.require(Role.ADMIN, Role.LOAN_OFFICER);
  }
}
