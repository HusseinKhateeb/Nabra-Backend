package com.nabra.backend.common.web;

import com.nabra.backend.security.principal.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
  private SecurityUtils() {}

  public static UserPrincipal currentPrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal up)) {
      throw new IllegalArgumentException("Unauthenticated");
    }
    return up;
  }
}
