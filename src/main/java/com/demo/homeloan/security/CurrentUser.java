package com.demo.homeloan.security;

import com.demo.homeloan.entity.*;
import com.demo.homeloan.exception.ApiException;
import com.demo.homeloan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUser {
  private final UserRepository users;

  public User get() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()
        || !(authentication.getPrincipal() instanceof UserDetails)) throw ApiException.forbidden();
    return users
        .findByEmail(authentication.getName())
        .filter(User::isEnabled)
        .orElseThrow(ApiException::forbidden);
  }

  public User require(Role... roles) {
    User u = get();
    for (Role r : roles) if (u.getRole() == r) return u;
    throw ApiException.forbidden();
  }
}
