package com.example.payraisemodule.dto;

import com.example.payraisemodule.domain.Employee;
import com.example.payraisemodule.domain.Role;

/** Shared "who is logged in" shape for {@code /api/login} and {@code /api/me}. */
public record AuthResponse(Long id, String email, String fullName, Role role) {
  public static AuthResponse from(Employee employee) {
    return new AuthResponse(
        employee.getId(), employee.getEmail(), employee.getFullName(), employee.getRole());
  }
}
