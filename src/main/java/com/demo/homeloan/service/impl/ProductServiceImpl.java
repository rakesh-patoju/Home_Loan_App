package com.demo.homeloan.service.impl;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.*;
import com.demo.homeloan.exception.ApiException;
import com.demo.homeloan.mapper.ApiMapper;
import com.demo.homeloan.repository.LoanProductRepository;
import com.demo.homeloan.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
  private final LoanProductRepository products;
  private final ApiMapper mapper;

  @Transactional(readOnly = true)
  public List<ProductResponse> list() {
    return products.findByActiveTrueOrderByIdAsc().stream().map(mapper::product).toList();
  }

  @Transactional(readOnly = true)
  public ProductResponse get(Long id) {
    return mapper.product(find(id));
  }

  private LoanProduct find(Long id) {
    return products
        .findById(id)
        .filter(LoanProduct::isActive)
        .orElseThrow(() -> ApiException.missing("Active loan product not found"));
  }

  @Transactional
  public ProductResponse create(ProductRequest r) {
    return mapper.product(products.save(fill(new LoanProduct(), r)));
  }

  @Transactional
  public ProductResponse update(Long id, ProductRequest r) {
    return mapper.product(
        fill(
            products.findById(id).orElseThrow(() -> ApiException.missing("Loan product not found")),
            r));
  }

  private LoanProduct fill(LoanProduct p, ProductRequest r) {
    if (r.minimumLoanAmount().compareTo(r.maximumLoanAmount()) > 0
        || r.minimumTenureMonths() > r.maximumTenureMonths())
      throw ApiException.bad("Minimum must not exceed maximum");
    p.setProductName(r.productName().trim());
    p.setMinimumLoanAmount(r.minimumLoanAmount());
    p.setMaximumLoanAmount(r.maximumLoanAmount());
    p.setInterestRate(r.interestRate());
    p.setMinimumTenureMonths(r.minimumTenureMonths());
    p.setMaximumTenureMonths(r.maximumTenureMonths());
    p.setDescription(r.description().trim());
    p.setActive(r.active());
    return p;
  }
}
