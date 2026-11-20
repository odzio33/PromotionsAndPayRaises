package com.example.payraisemodule.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.payraisemodule.domain.Employee;
import com.example.payraisemodule.domain.Position;
import com.example.payraisemodule.domain.Role;
import com.example.payraisemodule.repository.EmployeeRepository;
import com.example.payraisemodule.repository.PositionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Closes the gap left by the controller/service tests: nothing else directly exercises the {@code
 * /api/login}/{@code /api/me} contract itself (valid/invalid credentials, session persistence,
 * enumeration-safe error messages).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {

  private static final String PASSWORD = "TestPass123!";

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @Autowired private PositionRepository positionRepository;

  @Autowired private EmployeeRepository employeeRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private Employee employee;

  @BeforeEach
  void setUp() {
    Position position =
        positionRepository.save(
            new Position("Backend Developer", new BigDecimal("8000"), new BigDecimal("12000")));
    employee =
        employeeRepository.save(
            new Employee(
                "employee@example.com",
                "Ann Employee",
                Role.EMPLOYEE,
                null,
                position,
                passwordEncoder.encode(PASSWORD)));
  }

  @Test
  void loginWithValidCredentialsReturnsTheAuthenticatedEmployee() throws Exception {
    String body = objectMapper.writeValueAsString(new LoginPayload(employee.getEmail(), PASSWORD));

    mockMvc
        .perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(employee.getEmail()))
        .andExpect(jsonPath("$.fullName").value("Ann Employee"))
        .andExpect(jsonPath("$.role").value("EMPLOYEE"));
  }

  @Test
  void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
    String body = objectMapper.writeValueAsString(new LoginPayload(employee.getEmail(), "wrong"));

    mockMvc
        .perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid email or password"));
  }

  @Test
  void loginWithUnknownEmailReturnsTheSameMessageAsAWrongPassword() throws Exception {
    String body = objectMapper.writeValueAsString(new LoginPayload("nobody@example.com", PASSWORD));

    mockMvc
        .perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("Invalid email or password"));
  }

  @Test
  void meWithNoSessionReturnsUnauthorized() throws Exception {
    mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void meWithAValidSessionReturnsTheLoggedInEmployee() throws Exception {
    String loginBody =
        objectMapper.writeValueAsString(new LoginPayload(employee.getEmail(), PASSWORD));
    var loginResult =
        mockMvc
            .perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
            .andExpect(status().isOk())
            .andReturn();
    var session = (MockHttpSession) loginResult.getRequest().getSession(false);

    mockMvc
        .perform(get("/api/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(employee.getEmail()));
  }

  private record LoginPayload(String email, String password) {}
}
