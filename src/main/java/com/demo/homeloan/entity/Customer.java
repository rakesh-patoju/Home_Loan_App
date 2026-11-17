package com.demo.homeloan.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "customers")
public class Customer {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(nullable = false, length = 15)
  private String mobile;

  @Column(nullable = false)
  private LocalDate dateOfBirth;

  @Column(nullable = false, length = 500)
  private String address;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EmploymentType employmentType;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal monthlyIncome;

  @OneToMany(mappedBy = "customer")
  private java.util.List<LoanApplication> applications = new java.util.ArrayList<>();
}
