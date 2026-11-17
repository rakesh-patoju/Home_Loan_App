package com.demo.homeloan.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "loan_documents")
public class LoanDocument {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(nullable = false)
  private LoanApplication application;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private DocumentType documentType;

  @Column(nullable = false, length = 150)
  private String originalFilename;

  @Column(nullable = false, unique = true, length = 100)
  private String storageName;

  @Column(nullable = false, length = 40)
  private String contentType;

  @Column(nullable = false)
  private long sizeBytes;

  @Column(nullable = false)
  private Instant uploadedAt = Instant.now();

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private VerificationStatus verificationStatus = VerificationStatus.PENDING;

  @ManyToOne(fetch = FetchType.LAZY)
  private User verifiedBy;

  private Instant verifiedAt;

  @Column(length = 1000)
  private String verificationRemarks;
}
