package com.demo.homeloan.repository;

import com.demo.homeloan.entity.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  List<User> findByRoleAndEnabledTrueOrderByIdAsc(Role role);

  Page<User> findByRole(Role role, Pageable pageable);

  long countByRole(Role role);
}
