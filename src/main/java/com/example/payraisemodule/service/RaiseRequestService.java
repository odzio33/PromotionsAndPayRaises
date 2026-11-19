package com.example.payraisemodule.service;

import com.example.payraisemodule.domain.Employee;
import com.example.payraisemodule.domain.RaiseRequest;
import com.example.payraisemodule.domain.RequestStatus;
import com.example.payraisemodule.repository.EmployeeRepository;
import com.example.payraisemodule.repository.RaiseRequestRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RaiseRequestService {

  private final RaiseRequestRepository raiseRequestRepository;
  private final EmployeeRepository employeeRepository;

  public RaiseRequestService(
      RaiseRequestRepository raiseRequestRepository, EmployeeRepository employeeRepository) {
    this.raiseRequestRepository = raiseRequestRepository;
    this.employeeRepository = employeeRepository;
  }

  /** FR-001/FR-002: submit a request, enforcing the one-completed-request-per-year limit. */
  @Transactional
  public RaiseRequest submit(Long employeeId, BigDecimal requestedAmount, String justification) {
    Employee employee =
        employeeRepository
            .findById(employeeId)
            .orElseThrow(() -> new NoSuchEmployeeException(employeeId));

    Instant oneYearAgo = Instant.now().minus(365, ChronoUnit.DAYS);
    boolean hasCompletedRequestThisYear =
        raiseRequestRepository.existsByEmployeeIdAndStatusInAndDecidedAtAfter(
            employeeId, List.of(RequestStatus.APPROVED, RequestStatus.REJECTED), oneYearAgo);
    if (hasCompletedRequestThisYear) {
      throw new YearlyLimitExceededException(employeeId);
    }

    RaiseRequest request = new RaiseRequest(employee, requestedAmount, justification);
    return raiseRequestRepository.save(request);
  }

  /** FR-003: employee's own status and history. */
  @Transactional(readOnly = true)
  public List<RaiseRequest> findForEmployee(Long employeeId) {
    return raiseRequestRepository.findByEmployeeIdOrderBySubmittedAtDesc(employeeId);
  }

  /**
   * FR-004/FR-005: pending requests for a manager's direct reports, with band + history available
   * via the employee association.
   */
  @Transactional(readOnly = true)
  public List<RaiseRequest> findPendingForManager(Long managerId) {
    return raiseRequestRepository.findByEmployeeManagerIdAndStatusOrderBySubmittedAtAsc(
        managerId, RequestStatus.PENDING);
  }

  /**
   * FR-006/FR-007/FR-008: manager approves or rejects, recording their justification and band
   * decision.
   */
  @Transactional
  public RaiseRequest decide(
      Long requestId,
      Long managerId,
      RequestStatus decision,
      com.example.payraisemodule.domain.DecisionType decisionType,
      String managerJustification) {
    RaiseRequest request = requireManagedBy(requestId, managerId);
    request.decide(decision, decisionType, managerJustification);
    return request;
  }

  /** FR-008: manager sends the request back to the employee with a comment. */
  @Transactional
  public RaiseRequest sendBack(Long requestId, Long managerId, String comment) {
    RaiseRequest request = requireManagedBy(requestId, managerId);
    request.sendBack(comment);
    return request;
  }

  private RaiseRequest requireManagedBy(Long requestId, Long managerId) {
    RaiseRequest request =
        raiseRequestRepository
            .findById(requestId)
            .orElseThrow(() -> new NoSuchRaiseRequestException(requestId));
    Employee reportsManager = request.getEmployee().getManager();
    if (reportsManager == null || !reportsManager.getId().equals(managerId)) {
      throw new NotTheAssignedManagerException(requestId, managerId);
    }
    return request;
  }

  public static class NoSuchEmployeeException extends RuntimeException {
    public NoSuchEmployeeException(Long id) {
      super("No employee with id " + id);
    }
  }

  public static class NoSuchRaiseRequestException extends RuntimeException {
    public NoSuchRaiseRequestException(Long id) {
      super("No raise request with id " + id);
    }
  }

  public static class YearlyLimitExceededException extends RuntimeException {
    public YearlyLimitExceededException(Long employeeId) {
      super(
          "Employee " + employeeId + " already has a completed raise request within the past year");
    }
  }

  public static class NotTheAssignedManagerException extends RuntimeException {
    public NotTheAssignedManagerException(Long requestId, Long managerId) {
      super("Manager " + managerId + " is not the assigned manager for request " + requestId);
    }
  }
}
