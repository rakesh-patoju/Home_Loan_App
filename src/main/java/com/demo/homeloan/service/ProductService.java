package com.demo.homeloan.service;

import com.demo.homeloan.dto.ApiDtos.*;
import java.util.List;

public interface ProductService {
  List<ProductResponse> list();

  ProductResponse get(Long id);

  ProductResponse create(ProductRequest r);

  ProductResponse update(Long id, ProductRequest r);
}
