package com.example.payraisemodule.controller;

import com.example.payraisemodule.domain.DecisionType;
import com.example.payraisemodule.domain.Employee;
import com.example.payraisemodule.domain.Position;
import com.example.payraisemodule.domain.RequestStatus;
import com.example.payraisemodule.domain.Role;
import com.example.payraisemodule.repository.EmployeeRepository;
import com.example.payraisemodule.repository.PositionRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of US-01 (PRD): an employee submits a raise request and
 * their manager can see it alongside the position's salary band and decide on
 * it. Exercised through the real HTTP + persistence stack (MockMvc + H2),
 * i.e. a test from the user's perspective rather than a unit test of a
 * single class.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RaiseRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Employee manager;
    private Employee employee;

    @BeforeEach
    void setUp() {
        Position position = positionRepository.save(
                new Position("Backend Developer", new BigDecimal("8000"), new BigDecimal("12000")));
        manager = employeeRepository.save(
                new Employee("manager@example.com", "Mo Manager", Role.MANAGER, null, position));
        employee = employeeRepository.save(
                new Employee("employee@example.com", "Ann Employee", Role.EMPLOYEE, manager, position));
    }

    @Test
    void employeeSubmitsRequestManagerSeesBandAndApprovesEndToEnd() throws Exception {
        // 1. Employee submits a raise request with a justification and requested amount (FR-001).
        String submitBody = objectMapper.writeValueAsString(new SubmitPayload(
                new BigDecimal("11000"), "Shipped the Q3 migration, led two juniors."));

        mockMvc.perform(post("/api/employees/{id}/raise-requests", employee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(submitBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        // 2. The manager sees it alongside the salary band for the employee's position (FR-004/FR-005).
        var pendingResult = mockMvc.perform(get("/api/managers/{id}/raise-requests/pending", manager.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].positionTitle").value("Backend Developer"))
                .andExpect(jsonPath("$[0].salaryBandMin").value(8000))
                .andExpect(jsonPath("$[0].salaryBandMax").value(12000))
                .andReturn();

        Long requestId = objectMapper.readTree(pendingResult.getResponse().getContentAsString())
                .get(0).get("id").asLong();

        // 3. The manager approves it with their own justification and a band decision (FR-006/FR-007/FR-008).
        String decisionBody = objectMapper.writeValueAsString(
                new DecisionPayload(RequestStatus.APPROVED, DecisionType.WITHIN_BAND, "Solid case, within band."));

        mockMvc.perform(post("/api/managers/{managerId}/raise-requests/{requestId}/decision",
                        manager.getId(), requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decisionBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.managerJustification").value("Solid case, within band."));

        // 4. The employee sees the completed, approved request in their own history (FR-003).
        mockMvc.perform(get("/api/employees/{id}/raise-requests", employee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    void secondRequestIsBlockedOnlyAfterAPriorRequestIsCompleted() throws Exception {
        String firstSubmission = objectMapper.writeValueAsString(
                new SubmitPayload(new BigDecimal("11000"), "First attempt."));

        mockMvc.perform(post("/api/employees/{id}/raise-requests", employee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstSubmission))
                .andExpect(status().isCreated());

        var pendingResult = mockMvc.perform(get("/api/managers/{id}/raise-requests/pending", manager.getId()))
                .andReturn();
        Long requestId = objectMapper.readTree(pendingResult.getResponse().getContentAsString())
                .get(0).get("id").asLong();

        // Sending the first request back must NOT consume the yearly limit (FR-002).
        String sendBackBody = objectMapper.writeValueAsString(new SendBackPayload("Add more detail, please."));
        mockMvc.perform(post("/api/managers/{managerId}/raise-requests/{requestId}/send-back",
                        manager.getId(), requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendBackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT_BACK"));

        String secondSubmission = objectMapper.writeValueAsString(
                new SubmitPayload(new BigDecimal("11500"), "Revised with more detail."));
        mockMvc.perform(post("/api/employees/{id}/raise-requests", employee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondSubmission))
                .andExpect(status().isCreated());

        // Now approve the (second, still-pending) request — this is the first COMPLETED request this year.
        var pendingAfterSecond = mockMvc.perform(get("/api/managers/{id}/raise-requests/pending", manager.getId()))
                .andReturn();
        Long secondRequestId = objectMapper.readTree(pendingAfterSecond.getResponse().getContentAsString())
                .get(0).get("id").asLong();

        String decisionBody = objectMapper.writeValueAsString(
                new DecisionPayload(RequestStatus.APPROVED, DecisionType.WITHIN_BAND, "Approved on resubmission."));
        mockMvc.perform(post("/api/managers/{managerId}/raise-requests/{requestId}/decision",
                        manager.getId(), secondRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decisionBody))
                .andExpect(status().isOk());

        // A third submission within the same year is now blocked (FR-002).
        String thirdSubmission = objectMapper.writeValueAsString(
                new SubmitPayload(new BigDecimal("12000"), "Trying again too soon."));
        mockMvc.perform(post("/api/employees/{id}/raise-requests", employee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(thirdSubmission))
                .andExpect(status().isConflict());
    }

    private record SubmitPayload(BigDecimal requestedAmount, String justification) {
    }

    private record DecisionPayload(RequestStatus decision, DecisionType decisionType, String managerJustification) {
    }

    private record SendBackPayload(String comment) {
    }
}
