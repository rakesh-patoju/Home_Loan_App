package com.demo.homeloan;

import static org.assertj.core.api.Assertions.*;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.service.impl.EligibilityServiceImpl;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EligibilityServiceTest {
  private final EligibilityServiceImpl service =
      new EligibilityServiceImpl(new BigDecimal("50"), new BigDecimal("25000"));

  private EligibilityRequest request(
      String income, String existing, String principal, int months, String rate) {
    return new EligibilityRequest(
        new BigDecimal(income),
        new BigDecimal(existing),
        new BigDecimal(principal),
        months,
        new BigDecimal(rate));
  }

  @Test
  void reducingBalanceFormula() {
    var r = service.check(request("100000", "5000", "3000000", 240, "8.5"));
    assertThat(r.estimatedMonthlyEmi()).isEqualByComparingTo("26034.70");
    assertThat(r.debtToIncomePercent()).isEqualByComparingTo("31.03");
    assertThat(r.eligible()).isTrue();
  }

  @Test
  void zeroInterest() {
    var r = service.check(request("50000", "0", "120000", 12, "0"));
    assertThat(r.estimatedMonthlyEmi()).isEqualByComparingTo("10000.00");
    assertThat(r.eligible()).isTrue();
  }

  @Test
  void includesExistingEmi() {
    assertThat(service.check(request("50000", "20000", "120000", 12, "0")).eligible()).isFalse();
  }

  @Test
  void minimumIncomeApplies() {
    assertThat(service.check(request("20000", "0", "12000", 12, "0")).eligible()).isFalse();
  }

  @Test
  void boundaryIncludesExactlyFiftyPercent() {
    assertThat(service.check(request("50000", "15000", "120000", 12, "0")).eligible()).isTrue();
  }

  @Test
  void unroundedRatioControlsDecision() {
    assertThat(service.check(request("50000", "15000.01", "120000", 12, "0")).eligible()).isFalse();
  }
}
