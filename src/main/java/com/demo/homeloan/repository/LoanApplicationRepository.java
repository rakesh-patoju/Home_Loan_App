package com.demo.homeloan.repository;

import com.demo.homeloan.entity.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
  Page<LoanApplication> findByCustomerId(Long customerId, Pageable pageable);

  Page<LoanApplication> findByAssignedOfficerId(Long officerId, Pageable pageable);

  List<LoanApplication> findByCustomerId(Long customerId);

  List<LoanApplication> findByAssignedOfficerId(Long officerId);

  Optional<LoanApplication> findByCustomerIdAndIdempotencyKey(Long customerId, String key);

  boolean existsByCustomerIdAndRequestFingerprintAndStatusNotIn(
      Long customerId, String fingerprint, Collection<ApplicationStatus> statuses);

  long countByAssignedOfficerIdAndStatusNotIn(
      Long officerId, Collection<ApplicationStatus> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from LoanApplication a where a.id=:id")
  Optional<LoanApplication> lockById(@Param("id") Long id);
}
