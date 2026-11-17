package com.demo.homeloan.service.impl;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.*;
import com.demo.homeloan.mapper.ApiMapper;
import com.demo.homeloan.repository.*;
import com.demo.homeloan.security.CurrentUser;
import com.demo.homeloan.service.DashboardService;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
  private final LoanApplicationRepository applications;
  private final CustomerRepository customers;
  private final LoanDocumentRepository documents;
  private final CurrentUser current;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public DashboardResponse get(Role role) {
    User u = current.require(role);
    List<LoanApplication> list =
        switch (role) {
          case ADMIN -> applications.findAll();
          case LOAN_OFFICER -> applications.findByAssignedOfficerId(u.getId());
          case CUSTOMER ->
              applications.findByCustomerId(
                  customers.findByUserId(u.getId()).orElseThrow().getId());
        };
    Set<ApplicationStatus> pending =
        Set.of(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.DOCUMENT_PENDING,
            ApplicationStatus.UNDER_REVIEW);
    Set<ApplicationStatus> approved =
        Set.of(
            ApplicationStatus.APPROVED, ApplicationStatus.SANCTIONED, ApplicationStatus.DISBURSED);
    long verification =
        list.stream()
            .filter(a -> pending.contains(a.getStatus()))
            .filter(this::needsVerification)
            .count();
    return new DashboardResponse(
        role == Role.ADMIN ? customers.count() : 0,
        list.size(),
        list.stream().filter(a -> pending.contains(a.getStatus())).count(),
        verification,
        list.stream().filter(a -> approved.contains(a.getStatus())).count(),
        list.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED).count(),
        list.stream()
            .sorted(
                Comparator.comparing(LoanApplication::getApplicationDate)
                    .reversed()
                    .thenComparing(LoanApplication::getId, Comparator.reverseOrder()))
            .limit(5)
            .map(mapper::application)
            .toList());
  }

  private boolean needsVerification(LoanApplication a) {
    Map<DocumentType, LoanDocument> latest = new EnumMap<>(DocumentType.class);
    documents
        .findByApplicationIdOrderByUploadedAtAscIdAsc(a.getId())
        .forEach(d -> latest.put(d.getDocumentType(), d));
    return Arrays.stream(DocumentType.values())
        .anyMatch(
            t ->
                !latest.containsKey(t)
                    || latest.get(t).getVerificationStatus() != VerificationStatus.VERIFIED);
  }
}
