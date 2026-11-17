package com.demo.homeloan.service.impl;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.*;
import com.demo.homeloan.exception.ApiException;
import com.demo.homeloan.mapper.ApiMapper;
import com.demo.homeloan.repository.*;
import com.demo.homeloan.security.CurrentUser;
import com.demo.homeloan.service.ApplicationService;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {
  private final LoanApplicationRepository applications;
  private final CustomerRepository customers;
  private final LoanProductRepository products;
  private final UserRepository users;
  private final LoanDocumentRepository documents;
  private final ApplicationStatusHistoryRepository histories;
  private final CurrentUser current;
  private final ApplicationAccess access;
  private final ApiMapper mapper;
  public static final Set<ApplicationStatus> CLOSED =
      Set.of(ApplicationStatus.REJECTED, ApplicationStatus.DISBURSED);

  public static Pageable pageable(int page, int size, String sort) {
    if (page < 0 || size < 1 || size > 100)
      throw ApiException.bad("page must be >=0; size must be 1..100");
    String[] p = sort.split(",");
    if (p.length != 2
        || !Set.of("id", "applicationDate", "requestedLoanAmount", "status").contains(p[0])
        || !Set.of("asc", "desc").contains(p[1]))
      throw ApiException.bad(
          "sort must be id,asc or applicationDate,desc (also requestedLoanAmount/status)");
    return PageRequest.of(
        page, size, Sort.by(Sort.Direction.fromString(p[1]), p[0]).and(Sort.by("id")));
  }

  public static String fingerprint(ApplicationRequest r) {
    try {
      String value =
          r.loanProductId()
              + "|"
              + r.requestedLoanAmount().stripTrailingZeros().toPlainString()
              + "|"
              + r.tenureMonths()
              + "|"
              + r.monthlyIncome().stripTrailingZeros().toPlainString()
              + "|"
              + r.employmentType()
              + "|"
              + r.existingEmi().stripTrailingZeros().toPlainString();
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional
  public ApplicationResponse submit(ApplicationRequest r, String key) {
    if (key == null || !key.matches("[A-Za-z0-9_-]{8,100}"))
      throw ApiException.bad(
          "Idempotency-Key must contain 8..100 letters, digits, hyphens or underscores");
    // Serialize submissions per customer; READ_COMMITTED sees the previous lock holder's commit.
    Customer c =
        customers
            .lockByUserId(current.require(Role.CUSTOMER).getId())
            .orElseThrow(() -> ApiException.missing("Customer not found"));
    String hash = fingerprint(r);
    // A retried HTTP request returns its original application, rather than inserting again.
    Optional<LoanApplication> prior =
        applications.findByCustomerIdAndIdempotencyKey(c.getId(), key);
    if (prior.isPresent()) {
      if (!prior.get().getRequestFingerprint().equals(hash))
        throw ApiException.conflict("Idempotency-Key already used for a different request");
      return mapper.application(prior.get());
    }
    LoanProduct p =
        products
            .findById(r.loanProductId())
            .filter(LoanProduct::isActive)
            .orElseThrow(() -> ApiException.missing("Active product not found"));
    if (r.requestedLoanAmount().compareTo(p.getMinimumLoanAmount()) < 0
        || r.requestedLoanAmount().compareTo(p.getMaximumLoanAmount()) > 0)
      throw ApiException.bad("Requested amount is outside the product limits");
    if (r.tenureMonths() < p.getMinimumTenureMonths()
        || r.tenureMonths() > p.getMaximumTenureMonths())
      throw ApiException.bad("Tenure is outside the product limits");
    if (applications.existsByCustomerIdAndRequestFingerprintAndStatusNotIn(c.getId(), hash, CLOSED))
      throw ApiException.conflict("An identical active application already exists");
    LoanApplication a = new LoanApplication();
    a.setReferenceNumber("HL-" + UUID.randomUUID().toString().replace("-", ""));
    a.setCustomer(c);
    a.setLoanProduct(p);
    a.setRequestedLoanAmount(r.requestedLoanAmount());
    a.setTenureMonths(r.tenureMonths());
    a.setMonthlyIncome(r.monthlyIncome());
    a.setEmploymentType(r.employmentType());
    a.setExistingEmi(r.existingEmi());
    a.setInterestRate(p.getInterestRate());
    a.setIdempotencyKey(key);
    a.setRequestFingerprint(hash);
    users.findByRoleAndEnabledTrueOrderByIdAsc(Role.LOAN_OFFICER).stream()
        .min(
            Comparator.comparingLong(
                o -> applications.countByAssignedOfficerIdAndStatusNotIn(o.getId(), CLOSED)))
        .ifPresent(a::setAssignedOfficer);
    applications.save(a);
    event(a, null, ApplicationStatus.SUBMITTED, "Application submitted", current.get());
    log.info("Application {} submitted", a.getReferenceNumber());
    return mapper.application(a);
  }

  @Transactional(readOnly = true)
  public ApplicationResponse get(Long id) {
    return mapper.application(access.read(id));
  }

  @Transactional(readOnly = true)
  public CustomerResponse customer(Long id) {
    access.staff();
    return mapper.customer(access.read(id).getCustomer());
  }

  @Transactional(readOnly = true)
  public PageResponse<ApplicationResponse> list(boolean mine, int page, int size, String sort) {
    User u = current.get();
    Pageable pg = pageable(page, size, sort);
    Page<LoanApplication> data;
    if (mine) {
      if (u.getRole() != Role.CUSTOMER) throw ApiException.forbidden();
      data =
          applications.findByCustomerId(
              customers.findByUserId(u.getId()).orElseThrow().getId(), pg);
    } else {
      access.staff();
      data =
          u.getRole() == Role.ADMIN
              ? applications.findAll(pg)
              : applications.findByAssignedOfficerId(u.getId(), pg);
    }
    return PageResponse.of(data.map(mapper::application));
  }

  @Transactional
  public ApplicationResponse status(Long id, StatusRequest r) {
    access.staff();
    LoanApplication a = access.lock(id);
    ApplicationStatus old = a.getStatus();
    Set<ApplicationStatus> allowed =
        switch (old) {
          case SUBMITTED ->
              Set.of(
                  ApplicationStatus.UNDER_REVIEW,
                  ApplicationStatus.DOCUMENT_PENDING,
                  ApplicationStatus.REJECTED);
          case DOCUMENT_PENDING ->
              Set.of(ApplicationStatus.UNDER_REVIEW, ApplicationStatus.REJECTED);
          case UNDER_REVIEW ->
              Set.of(
                  ApplicationStatus.DOCUMENT_PENDING,
                  ApplicationStatus.APPROVED,
                  ApplicationStatus.REJECTED);
          case APPROVED -> Set.of(ApplicationStatus.SANCTIONED);
          case SANCTIONED -> Set.of(ApplicationStatus.DISBURSED);
          default -> Set.of();
        };
    if (!allowed.contains(r.status()))
      throw ApiException.conflict("Invalid transition: " + old + " -> " + r.status());
    if (r.status() == ApplicationStatus.SANCTIONED || r.status() == ApplicationStatus.DISBURSED)
      current.require(Role.ADMIN);
    if (r.status() == ApplicationStatus.APPROVED) requireVerifiedDocuments(a);
    a.setStatus(r.status());
    event(a, old, r.status(), r.remarks().trim(), current.get());
    log.info("Application {} changed {} -> {}", a.getReferenceNumber(), old, r.status());
    return mapper.application(a);
  }

  // New uploads supersede earlier files in the same category for approval purposes.
  private void requireVerifiedDocuments(LoanApplication a) {
    Map<DocumentType, LoanDocument> latest = new EnumMap<>(DocumentType.class);
    documents
        .findByApplicationIdOrderByUploadedAtAscIdAsc(a.getId())
        .forEach(d -> latest.put(d.getDocumentType(), d));
    for (DocumentType t : DocumentType.values())
      if (!latest.containsKey(t)
          || latest.get(t).getVerificationStatus() != VerificationStatus.VERIFIED)
        throw ApiException.conflict("Latest document must be verified for: " + t);
  }

  @Transactional
  public ApplicationResponse assign(Long id, AssignmentRequest r) {
    current.require(Role.ADMIN);
    LoanApplication a = access.lock(id);
    if (CLOSED.contains(a.getStatus()))
      throw ApiException.conflict("Cannot reassign a closed application");
    User o =
        users
            .findById(r.officerId())
            .filter(u -> u.getRole() == Role.LOAN_OFFICER && u.isEnabled())
            .orElseThrow(() -> ApiException.bad("Active loan officer not found"));
    a.setAssignedOfficer(o);
    event(a, a.getStatus(), a.getStatus(), "Assigned to officer ID " + o.getId(), current.get());
    return mapper.application(a);
  }

  @Transactional
  public HistoryResponse remark(Long id, RemarkRequest r) {
    access.staff();
    LoanApplication a = access.lock(id);
    return mapper.history(
        event(a, a.getStatus(), a.getStatus(), r.remarks().trim(), current.get()));
  }

  @Transactional(readOnly = true)
  public List<HistoryResponse> history(Long id) {
    access.read(id);
    return histories.findByApplicationIdOrderByChangedAtAscIdAsc(id).stream()
        .map(mapper::history)
        .toList();
  }

  private ApplicationStatusHistory event(
      LoanApplication a, ApplicationStatus from, ApplicationStatus to, String remarks, User actor) {
    ApplicationStatusHistory h = new ApplicationStatusHistory();
    h.setApplication(a);
    h.setFromStatus(from);
    h.setToStatus(to);
    h.setRemarks(remarks);
    h.setChangedBy(actor);
    return histories.save(h);
  }
}
