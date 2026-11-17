package com.demo.homeloan.controller;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.*;
import com.demo.homeloan.service.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class DocumentController {
  private final DocumentService service;

  @PostMapping(
      value = "/api/loan-applications/{id}/documents",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('CUSTOMER')")
  public DocumentResponse upload(
      @PathVariable Long id,
      @RequestParam DocumentType documentType,
      @RequestPart("file") MultipartFile file) {
    return service.upload(id, documentType, file);
  }

  @GetMapping("/api/loan-applications/{id}/documents")
  public List<DocumentResponse> list(@PathVariable Long id) {
    return service.list(id);
  }

  @PutMapping("/api/documents/{id}/verify")
  @PreAuthorize("hasAnyRole('ADMIN','LOAN_OFFICER')")
  public DocumentResponse verify(@PathVariable Long id, @Valid @RequestBody VerifyRequest r) {
    return service.verify(id, r);
  }

  @GetMapping("/api/documents/{id}/download")
  public ResponseEntity<Resource> download(@PathVariable Long id) {
    DocumentService.Download d = service.download(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(d.contentType()))
        .contentLength(d.size())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(d.filename()).build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header("X-Content-Type-Options", "nosniff")
        .body(d.resource());
  }
}
