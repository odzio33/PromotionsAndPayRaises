package com.example.payraisemodule.dto;

import com.example.payraisemodule.domain.DecisionType;
import com.example.payraisemodule.domain.RaiseRequest;
import com.example.payraisemodule.domain.RequestStatus;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * FR-003/FR-004/FR-005: carries the request plus the position's salary band so a viewer (employee
 * or manager) never has to look it up separately.
 */
public record RaiseRequestResponse(
    Long id,
    Long employeeId,
    String employeeName,
    String positionTitle,
    BigDecimal salaryBandMin,
    BigDecimal salaryBandMax,
    BigDecimal requestedAmount,
    String justification,
    RequestStatus status,
    DecisionType decisionType,
    String managerJustification,
    String sendBackComment,
    Instant submittedAt,
    Instant decidedAt) {
  public static RaiseRequestResponse from(RaiseRequest request) {
    var employee = request.getEmployee();
    var position = employee.getPosition();
    return new RaiseRequestResponse(
        request.getId(),
        employee.getId(),
        employee.getFullName(),
        position.getTitle(),
        position.getSalaryBandMin(),
        position.getSalaryBandMax(),
        request.getRequestedAmount(),
        request.getJustification(),
        request.getStatus(),
        request.getDecisionType(),
        request.getManagerJustification(),
        request.getSendBackComment(),
        request.getSubmittedAt(),
        request.getDecidedAt());
  }
}
