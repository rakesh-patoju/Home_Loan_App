package com.demo.homeloan.repository;

import com.demo.homeloan.entity.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ApplicationStatusHistoryRepository
    extends JpaRepository<ApplicationStatusHistory, Long> {
  List<ApplicationStatusHistory> findByApplicationIdOrderByChangedAtAscIdAsc(Long id);
}
