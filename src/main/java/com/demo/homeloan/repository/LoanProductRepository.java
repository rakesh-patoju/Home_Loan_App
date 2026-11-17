package com.demo.homeloan.repository;

import com.demo.homeloan.entity.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
  List<LoanProduct> findByActiveTrueOrderByIdAsc();
}
