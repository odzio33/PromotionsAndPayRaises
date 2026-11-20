package com.example.payraisemodule.security;

import com.example.payraisemodule.repository.EmployeeRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/** Loads a {@link UserDetails} by email for Spring Security to authenticate against. */
@Service
public class EmployeeUserDetailsService implements UserDetailsService {

  private final EmployeeRepository employeeRepository;

  public EmployeeUserDetailsService(EmployeeRepository employeeRepository) {
    this.employeeRepository = employeeRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return employeeRepository
        .findByEmail(email)
        .map(EmployeeUserDetails::new)
        .orElseThrow(() -> new UsernameNotFoundException("No employee with email " + email));
  }
}
