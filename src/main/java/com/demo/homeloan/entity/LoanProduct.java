package com.demo.homeloan.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "loan_products")
public class LoanProduct {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String productName;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal minimumLoanAmount;

  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal maximumLoanAmount;

  @Column(nullable = false, precision = 5, scale = 2)
  private BigDecimal interestRate;

  @Column(nullable = false)
  private int minimumTenureMonths;

  @Column(nullable = false)
  private int maximumTenureMonths;

  @Column(nullable = false, length = 1000)
  private String description;

  @Column(nullable = false)
  private boolean active = true;
}
