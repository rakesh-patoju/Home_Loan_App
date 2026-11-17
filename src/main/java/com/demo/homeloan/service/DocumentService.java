package com.demo.homeloan.service;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.DocumentType;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {
  record Download(Resource resource, String filename, String contentType, long size) {}

  DocumentResponse upload(Long applicationId, DocumentType type, MultipartFile file);

  List<DocumentResponse> list(Long applicationId);

  DocumentResponse verify(Long id, VerifyRequest request);

  Download download(Long id);
}
