package com.demo.homeloan.repository;

import com.demo.homeloan.entity.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
  Optional<Customer> findByUserId(Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Customer c where c.user.id=:userId")
  Optional<Customer> lockByUserId(@Param("userId") Long userId);
}
