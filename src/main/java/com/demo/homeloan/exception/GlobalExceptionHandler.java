package com.demo.homeloan.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.*;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  public record ErrorResponse(
      Instant timestamp,
      int status,
      String message,
      String path,
      Map<String, String> fieldErrors) {}

  private ResponseEntity<ErrorResponse> response(
      HttpStatus status, String message, HttpServletRequest r, Map<String, String> fields) {
    return ResponseEntity.status(status)
        .body(new ErrorResponse(Instant.now(), status.value(), message, r.getRequestURI(), fields));
  }

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ErrorResponse> api(ApiException e, HttpServletRequest r) {
    return response(e.getStatus(), e.getMessage(), r, Map.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> validation(
      MethodArgumentNotValidException e, HttpServletRequest r) {
    Map<String, String> fields = new LinkedHashMap<>();
    e.getBindingResult()
        .getFieldErrors()
        .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
    return response(HttpStatus.BAD_REQUEST, "Validation failed", r, fields);
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    ConstraintViolationException.class,
    MissingServletRequestParameterException.class,
    MissingServletRequestPartException.class,
    org.springframework.web.bind.MissingRequestHeaderException.class
  })
  ResponseEntity<ErrorResponse> bad(Exception e, HttpServletRequest r) {
    return response(HttpStatus.BAD_REQUEST, "Invalid or missing request fields", r, Map.of());
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<ErrorResponse> denied(Exception e, HttpServletRequest r) {
    return response(HttpStatus.FORBIDDEN, "Access denied", r, Map.of());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ErrorResponse> size(Exception e, HttpServletRequest r) {
    return response(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum file size is 5 MB", r, Map.of());
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class,
    PessimisticLockingFailureException.class
  })
  ResponseEntity<ErrorResponse> conflict(Exception e, HttpServletRequest r) {
    return response(
        HttpStatus.CONFLICT,
        "Conflicting data or concurrent update; refresh and retry",
        r,
        Map.of());
  }

  @ExceptionHandler({
    org.springframework.web.servlet.resource.NoResourceFoundException.class,
    org.springframework.web.HttpRequestMethodNotSupportedException.class,
    org.springframework.web.HttpMediaTypeNotSupportedException.class,
    org.springframework.web.HttpMediaTypeNotAcceptableException.class
  })
  ResponseEntity<ErrorResponse> http(Exception e, HttpServletRequest r) {
    var error = (org.springframework.web.ErrorResponse) e;
    return response(
        HttpStatus.valueOf(error.getStatusCode().value()),
        error.getBody().getDetail(),
        r,
        Map.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorResponse> other(Exception e, HttpServletRequest r) {
    log.error("Unhandled request failure on {}", r.getRequestURI(), e);
    return response(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", r, Map.of());
  }
}
