package com.demo.homeloan.service;

import com.demo.homeloan.dto.ApiDtos.*;
import java.util.List;

public interface ApplicationService {
  ApplicationResponse submit(ApplicationRequest r, String key);

  ApplicationResponse get(Long id);

  PageResponse<ApplicationResponse> list(boolean mine, int page, int size, String sort);

  ApplicationResponse status(Long id, StatusRequest r);

  ApplicationResponse assign(Long id, AssignmentRequest r);

  HistoryResponse remark(Long id, RemarkRequest r);

  List<HistoryResponse> history(Long id);

  CustomerResponse customer(Long id);
}
