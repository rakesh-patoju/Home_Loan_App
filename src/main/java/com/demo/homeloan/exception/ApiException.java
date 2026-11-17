package com.demo.homeloan.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
  private final HttpStatus status;

  public ApiException(HttpStatus status, String message) {
    super(message);
    this.status = status;
  }

  public static ApiException bad(String s) {
    return new ApiException(HttpStatus.BAD_REQUEST, s);
  }

  public static ApiException missing(String s) {
    return new ApiException(HttpStatus.NOT_FOUND, s);
  }

  public static ApiException forbidden() {
    return new ApiException(HttpStatus.FORBIDDEN, "Access denied");
  }

  public static ApiException conflict(String s) {
    return new ApiException(HttpStatus.CONFLICT, s);
  }
}
