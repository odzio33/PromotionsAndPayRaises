package com.example.payraisemodule.controller;

import com.example.payraisemodule.dto.AuthResponse;
import com.example.payraisemodule.dto.LoginRequest;
import com.example.payraisemodule.security.EmployeeUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes JSON login/me endpoints. Logout needs no custom controller code — Spring Security's own
 * filter at {@code /api/logout} handles it entirely.
 */
@RestController
@RequestMapping("/api")
public class AuthController {

  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository securityContextRepository =
      new HttpSessionSecurityContextRepository();

  public AuthController(AuthenticationManager authenticationManager) {
    this.authenticationManager = authenticationManager;
  }

  @PostMapping("/login")
  public AuthResponse login(
      @Valid @RequestBody LoginRequest body,
      HttpServletRequest request,
      HttpServletResponse response) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(body.email(), body.password()));

    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    securityContextRepository.saveContext(context, request, response);

    var principal = (EmployeeUserDetails) authentication.getPrincipal();
    return AuthResponse.from(principal.getEmployee());
  }

  @GetMapping("/me")
  public AuthResponse me(@AuthenticationPrincipal EmployeeUserDetails principal) {
    return AuthResponse.from(principal.getEmployee());
  }
}
