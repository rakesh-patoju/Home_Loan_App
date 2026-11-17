package com.demo.homeloan.service.impl;

import com.demo.homeloan.dto.ApiDtos.*;
import com.demo.homeloan.entity.*;
import com.demo.homeloan.exception.ApiException;
import com.demo.homeloan.mapper.ApiMapper;
import com.demo.homeloan.repository.LoanDocumentRepository;
import com.demo.homeloan.security.CurrentUser;
import com.demo.homeloan.service.DocumentService;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class DocumentServiceImpl implements DocumentService {
  private final LoanDocumentRepository documents;
  private final ApplicationAccess access;
  private final CurrentUser current;
  private final ApiMapper mapper;
  private final Path root;

  public DocumentServiceImpl(
      LoanDocumentRepository documents,
      ApplicationAccess access,
      CurrentUser current,
      ApiMapper mapper,
      @Value("${app.upload-dir}") String directory)
      throws IOException {
    this.documents = documents;
    this.access = access;
    this.current = current;
    this.mapper = mapper;
    Path p = Path.of(directory).toAbsolutePath().normalize();
    Files.createDirectories(p);
    this.root = p.toRealPath();
  }

  private void editable(LoanApplication a) {
    if (!Set.of(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.DOCUMENT_PENDING,
            ApplicationStatus.UNDER_REVIEW)
        .contains(a.getStatus()))
      throw ApiException.conflict("Documents cannot change after decision");
  }

  @Transactional
  public DocumentResponse upload(Long id, DocumentType type, MultipartFile file) {
    current.require(Role.CUSTOMER);
    LoanApplication a = access.lock(id);
    editable(a);
    if (file.isEmpty() || file.getSize() > 5L * 1024 * 1024)
      throw ApiException.bad("File must be nonempty and at most 5 MB");
    String original = Optional.ofNullable(file.getOriginalFilename()).orElse("").replace('\\', '/');
    original = original.substring(original.lastIndexOf('/') + 1).replaceAll("[^a-zA-Z0-9._-]", "_");
    if (original.length() > 150) original = original.substring(original.length() - 150);
    int dot = original.lastIndexOf('.');
    String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
    if (!Set.of("pdf", "jpg", "jpeg", "png").contains(ext))
      throw ApiException.bad("Only PDF, JPG and PNG are supported");
    try {
      byte[] data = file.getBytes();
      String mime = validate(data, ext);
      String name = UUID.randomUUID() + "." + ext;
      Path destination = root.resolve(name);
      Files.write(destination, data, StandardOpenOption.CREATE_NEW);
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              if (status != STATUS_COMMITTED)
                try {
                  Files.deleteIfExists(destination);
                } catch (IOException e) {
                  log.warn("Could not clean rolled-back upload {}", name);
                }
            }
          });
      LoanDocument d = new LoanDocument();
      d.setApplication(a);
      d.setDocumentType(type);
      d.setOriginalFilename(original);
      d.setStorageName(name);
      d.setContentType(mime);
      d.setSizeBytes(data.length);
      return mapper.document(documents.save(d));
    } catch (IOException e) {
      throw new UncheckedIOException("Document storage failed", e);
    }
  }

  private String validate(byte[] bytes, String ext) throws IOException {
    if (ext.equals("pdf")) {
      String head = new String(bytes, 0, Math.min(8, bytes.length), StandardCharsets.US_ASCII);
      String tail =
          new String(
              bytes,
              Math.max(0, bytes.length - 1024),
              Math.min(1024, bytes.length),
              StandardCharsets.ISO_8859_1);
      if (!head.startsWith("%PDF-") || !tail.contains("%%EOF"))
        throw ApiException.bad("File does not contain a valid PDF signature and end marker");
      return "application/pdf";
    }
    boolean png =
        bytes.length >= 8
            && Arrays.equals(
                Arrays.copyOf(bytes, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
    boolean jpg =
        bytes.length >= 3
            && bytes[0] == (byte) 255
            && bytes[1] == (byte) 216
            && bytes[2] == (byte) 255;
    if ((ext.equals("png") && !png) || (!ext.equals("png") && !jpg))
      throw ApiException.bad("File content does not match its extension");
    try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) throw ApiException.bad("Unreadable image");
      var reader = readers.next();
      try {
        reader.setInput(input);
        long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
        if (pixels < 1 || pixels > 20_000_000)
          throw ApiException.bad("Image must contain at most 20 million pixels");
        if (reader.read(0) == null) throw ApiException.bad("Unreadable image");
      } catch (javax.imageio.IIOException e) {
        throw ApiException.bad("Corrupt image");
      } finally {
        reader.dispose();
      }
    }
    return png ? "image/png" : "image/jpeg";
  }

  @Transactional(readOnly = true)
  public List<DocumentResponse> list(Long id) {
    access.read(id);
    return documents.findByApplicationIdOrderByUploadedAtAscIdAsc(id).stream()
        .map(mapper::document)
        .toList();
  }

  @Transactional
  public DocumentResponse verify(Long id, VerifyRequest r) {
    access.staff();
    LoanDocument d =
        documents.findById(id).orElseThrow(() -> ApiException.missing("Document not found"));
    LoanApplication a = access.lock(d.getApplication().getId());
    editable(a);
    if (r.status() == VerificationStatus.PENDING)
      throw ApiException.bad("Verification must be VERIFIED or REJECTED");
    LoanDocument latest =
        documents.findByApplicationIdOrderByUploadedAtAscIdAsc(a.getId()).stream()
            .filter(x -> x.getDocumentType() == d.getDocumentType())
            .reduce((x, y) -> y)
            .orElseThrow();
    if (!latest.getId().equals(id))
      throw ApiException.conflict("Verify the latest uploaded document for this category");
    d.setVerificationStatus(r.status());
    d.setVerificationRemarks(r.remarks().trim());
    d.setVerifiedAt(Instant.now());
    d.setVerifiedBy(current.get());
    return mapper.document(d);
  }

  @Transactional(readOnly = true)
  public Download download(Long id) {
    LoanDocument d =
        documents.findById(id).orElseThrow(() -> ApiException.missing("Document not found"));
    access.read(d.getApplication().getId());
    Path path = root.resolve(d.getStorageName()).normalize();
    if (!path.getParent().equals(root) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
      throw ApiException.missing("Stored document not found");
    return new Download(
        new FileSystemResource(path),
        d.getOriginalFilename(),
        d.getContentType(),
        d.getSizeBytes());
  }
}
