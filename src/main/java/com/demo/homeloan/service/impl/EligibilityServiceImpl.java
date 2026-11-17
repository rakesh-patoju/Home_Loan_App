package com.demo.homeloan.service.impl;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.service.EligibilityService;
import java.math.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EligibilityServiceImpl implements EligibilityService {
  private final BigDecimal maxDti, minIncome;

  public EligibilityServiceImpl(
      @Value("${app.max-dti-percent}") BigDecimal maxDti,
      @Value("${app.min-monthly-income}") BigDecimal minIncome) {
    if (maxDti.signum() <= 0
        || maxDti.compareTo(BigDecimal.valueOf(100)) > 0
        || minIncome.signum() < 0)
      throw new IllegalArgumentException("Invalid eligibility criteria");
    this.maxDti = maxDti;
    this.minIncome = minIncome;
  }

  public EligibilityResponse check(EligibilityRequest r) {
    MathContext mc = MathContext.DECIMAL128;
    BigDecimal monthlyRate = r.interestRate().divide(BigDecimal.valueOf(1200), mc);
    BigDecimal raw;
    if (monthlyRate.signum() == 0)
      raw = r.requestedLoanAmount().divide(BigDecimal.valueOf(r.tenureMonths()), mc);
    else {
      BigDecimal factor = BigDecimal.ONE.add(monthlyRate, mc).pow(r.tenureMonths(), mc);
      raw =
          r.requestedLoanAmount()
              .multiply(monthlyRate, mc)
              .multiply(factor, mc)
              .divide(factor.subtract(BigDecimal.ONE), mc);
    }
    BigDecimal dti =
        r.existingEmi().add(raw).multiply(BigDecimal.valueOf(100)).divide(r.monthlyIncome(), mc);
    return new EligibilityResponse(
        raw.setScale(2, RoundingMode.HALF_UP),
        dti.setScale(2, RoundingMode.HALF_UP),
        maxDti,
        minIncome,
        r.monthlyIncome().compareTo(minIncome) >= 0 && dti.compareTo(maxDti) <= 0,
        "Preliminary estimate only. This is not bank approval, a credit check or a loan offer.");
  }
}
