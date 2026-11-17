package com.demo.homeloan.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "loan_applications",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_customer_idempotency",
            columnNames = {"customer_id", "idempotency_key"}))
public class LoanApplication {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 40)
  private String referenceNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private LoanProduct loanProduct;

  @ManyToOne(fetch = FetchType.LAZY)
  private User assignedOfficer;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal requestedLoanAmount;

  @Column(nullable = false)
  private int tenureMonths;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal monthlyIncome;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EmploymentType employmentType;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal existingEmi;

  @Column(nullable = false, precision = 5, scale = 2)
  private BigDecimal interestRate;

  @Column(nullable = false)
  private Instant applicationDate = Instant.now();

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ApplicationStatus status = ApplicationStatus.SUBMITTED;

  @Column(nullable = false, length = 100)
  private String idempotencyKey;

  @Column(nullable = false, length = 64)
  private String requestFingerprint;

  @Version private long version;

  @OneToMany(mappedBy = "application")
  private java.util.List<LoanDocument> documents = new java.util.ArrayList<>();

  @OneToMany(mappedBy = "application")
  private java.util.List<ApplicationStatusHistory> statusHistory = new java.util.ArrayList<>();
}
