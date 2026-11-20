package com.example.payraisemodule.security;

import com.example.payraisemodule.domain.Employee;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Adapts {@link Employee} to Spring Security's {@link UserDetails} contract. */
public class EmployeeUserDetails implements UserDetails {

  private final Employee employee;

  public EmployeeUserDetails(Employee employee) {
    this.employee = employee;
  }

  public Employee getEmployee() {
    return employee;
  }

  public Long getEmployeeId() {
    return employee.getId();
  }

  @Override
  public Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()));
  }

  @Override
  public String getPassword() {
    return employee.getPasswordHash();
  }

  @Override
  public String getUsername() {
    return employee.getEmail();
  }
}
