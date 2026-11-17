package com.demo.homeloan.configuration;

import com.demo.homeloan.entity.*;
import com.demo.homeloan.repository.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataInitializer implements CommandLineRunner {
  private final UserRepository users;
  private final CustomerRepository customers;
  private final LoanProductRepository products;
  private final LoanApplicationRepository applications;
  private final ApplicationStatusHistoryRepository histories;
  private final PasswordEncoder encoder;

  @Value("${app.demo.password}")
  private String password;

  @Override
  @Transactional
  public void run(String... args) {
    if (users.count() != 0) {
      log.info("Demo seed skipped: users already exist");
      return;
    }
    if (password.length() < 10
        || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
      throw new IllegalArgumentException(
          "Set DEMO_PASSWORD to 10+ characters (max 72 UTF-8 bytes), or disable demo seed");
    String hash = encoder.encode(password);
    User admin = user("Demo Administrator", "admin@homeloan.demo", Role.ADMIN, hash);
    User first = user("Anita Demo", "officer1@homeloan.demo", Role.LOAN_OFFICER, hash);
    User second = user("Vikram Demo", "officer2@homeloan.demo", Role.LOAN_OFFICER, hash);
    List<LoanProduct> ps = new ArrayList<>();
    String[] names = {"Standard Home Loan", "Affordable Home Loan", "Home Renovation Loan"};
    for (int i = 0; i < 3; i++) {
      LoanProduct p = new LoanProduct();
      p.setProductName(names[i]);
      p.setMinimumLoanAmount(new BigDecimal("100000"));
      p.setMaximumLoanAmount(new BigDecimal(i == 1 ? "5000000" : "20000000"));
      p.setInterestRate(new BigDecimal(i == 0 ? "8.50" : i == 1 ? "8.00" : "9.00"));
      p.setMinimumTenureMonths(12);
      p.setMaximumTenureMonths(i == 2 ? 180 : 360);
      p.setDescription(
          "Fictional demonstration product. Subject to manual review; not a real bank offer.");
      ps.add(products.save(p));
    }
    String[] names2 = {"Aarav Demo", "Diya Demo", "Kabir Demo", "Meera Demo", "Neel Demo"};
    for (int i = 0; i < 5; i++) {
      User u = user(names2[i], "customer" + (i + 1) + "@homeloan.demo", Role.CUSTOMER, hash);
      Customer c = new Customer();
      c.setUser(u);
      c.setMobile("900000000" + i);
      c.setDateOfBirth(LocalDate.of(1990 + i, 6, 15));
      c.setAddress((i + 1) + " Demo Street, Fictional City, India");
      c.setEmploymentType(EmploymentType.SALARIED);
      c.setMonthlyIncome(new BigDecimal("80000"));
      customers.save(c);
      for (int k = 0; k < 2; k++) {
        LoanApplication a = new LoanApplication();
        a.setReferenceNumber("HL-DEMO-" + (i * 2 + k + 1));
        a.setCustomer(c);
        a.setLoanProduct(ps.get((i + k) % 3));
        a.setAssignedOfficer(k == 0 ? first : second);
        a.setRequestedLoanAmount(
            new BigDecimal("2000000").add(BigDecimal.valueOf(i * 100000L + k * 50000L)));
        a.setTenureMonths(120);
        a.setMonthlyIncome(c.getMonthlyIncome());
        a.setEmploymentType(c.getEmploymentType());
        a.setExistingEmi(new BigDecimal("5000"));
        a.setInterestRate(a.getLoanProduct().getInterestRate());
        a.setIdempotencyKey("demo-seed-" + (i * 2 + k + 1));
        a.setRequestFingerprint(
            com.demo.homeloan.service.impl.ApplicationServiceImpl.fingerprint(
                new com.demo.homeloan.dto.ApiDtos.ApplicationRequest(
                    a.getLoanProduct().getId(),
                    a.getRequestedLoanAmount(),
                    a.getTenureMonths(),
                    a.getMonthlyIncome(),
                    a.getEmploymentType(),
                    a.getExistingEmi())));
        a.setApplicationDate(Instant.now().minusSeconds((10 - i * 2 - k) * 86400L));
        applications.save(a);
        history(
            a,
            null,
            ApplicationStatus.SUBMITTED,
            "Fictional demo application submitted",
            u,
            a.getApplicationDate());
        ApplicationStatus status =
            switch ((i * 2 + k) % 4) {
              case 1 -> ApplicationStatus.DOCUMENT_PENDING;
              case 2 -> ApplicationStatus.UNDER_REVIEW;
              case 3 -> ApplicationStatus.REJECTED;
              default -> ApplicationStatus.SUBMITTED;
            };
        if (status != ApplicationStatus.SUBMITTED) {
          a.setStatus(status);
          history(
              a,
              ApplicationStatus.SUBMITTED,
              status,
              status == ApplicationStatus.REJECTED
                  ? "Demo rejection: application information incomplete"
                  : "Demo review: upload the required fictional documents",
              a.getAssignedOfficer(),
              a.getApplicationDate().plusSeconds(3600));
        }
      }
    }
    log.info(
        "Seeded 5 customers, 2 officers, 1 admin, 3 products and 10 applications. Password comes"
            + " from your local configuration.");
  }

  private User user(String name, String email, Role role, String hash) {
    User u = new User();
    u.setFullName(name);
    u.setEmail(email);
    u.setRole(role);
    u.setPasswordHash(hash);
    return users.save(u);
  }

  private void history(
      LoanApplication a,
      ApplicationStatus from,
      ApplicationStatus to,
      String text,
      User actor,
      Instant time) {
    ApplicationStatusHistory h = new ApplicationStatusHistory();
    h.setApplication(a);
    h.setFromStatus(from);
    h.setToStatus(to);
    h.setRemarks(text);
    h.setChangedBy(actor);
    h.setChangedAt(time);
    histories.save(h);
  }
}
