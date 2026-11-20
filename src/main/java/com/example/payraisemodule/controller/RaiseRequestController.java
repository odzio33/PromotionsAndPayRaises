package com.example.payraisemodule.controller;

import com.example.payraisemodule.dto.DecisionRequest;
import com.example.payraisemodule.dto.RaiseRequestResponse;
import com.example.payraisemodule.dto.SendBackRequest;
import com.example.payraisemodule.dto.SubmitRaiseRequest;
import com.example.payraisemodule.security.EmployeeUserDetails;
import com.example.payraisemodule.service.RaiseRequestService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Covers FR-001 through FR-008 for the employee -> manager leg (MVP scope; HR/CEO stages are out of
 * scope per PRD ## Non-Goals).
 *
 * <p>Identity is derived from the authenticated session ({@link EmployeeUserDetails}), never from a
 * URL-supplied id, per PRD ## Access Control: login required.
 */
@RestController
@RequestMapping("/api")
public class RaiseRequestController {

  private final RaiseRequestService raiseRequestService;

  public RaiseRequestController(RaiseRequestService raiseRequestService) {
    this.raiseRequestService = raiseRequestService;
  }

  /** FR-001: employee submits a raise request. */
  @PreAuthorize("hasRole('EMPLOYEE')")
  @PostMapping("/raise-requests")
  public ResponseEntity<RaiseRequestResponse> submit(
      @AuthenticationPrincipal EmployeeUserDetails principal,
      @Valid @RequestBody SubmitRaiseRequest body) {
    var request =
        raiseRequestService.submit(
            principal.getEmployeeId(), body.requestedAmount(), body.justification());
    return ResponseEntity.status(HttpStatus.CREATED).body(RaiseRequestResponse.from(request));
  }

  /** FR-003: employee views their own request status and history. */
  @PreAuthorize("hasRole('EMPLOYEE')")
  @GetMapping("/raise-requests/mine")
  public List<RaiseRequestResponse> findForEmployee(
      @AuthenticationPrincipal EmployeeUserDetails principal) {
    return raiseRequestService.findForEmployee(principal.getEmployeeId()).stream()
        .map(RaiseRequestResponse::from)
        .toList();
  }

  /**
   * FR-004/FR-005: manager views pending requests from direct reports, with band + history
   * available on each.
   */
  @PreAuthorize("hasRole('MANAGER')")
  @GetMapping("/raise-requests/pending")
  public List<RaiseRequestResponse> findPendingForManager(
      @AuthenticationPrincipal EmployeeUserDetails principal) {
    return raiseRequestService.findPendingForManager(principal.getEmployeeId()).stream()
        .map(RaiseRequestResponse::from)
        .toList();
  }

  /**
   * FR-006/FR-007/FR-008: manager approves or rejects a request with a justification and band
   * decision.
   */
  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping("/raise-requests/{requestId}/decision")
  public RaiseRequestResponse decide(
      @AuthenticationPrincipal EmployeeUserDetails principal,
      @PathVariable Long requestId,
      @Valid @RequestBody DecisionRequest body) {
    var request =
        raiseRequestService.decide(
            requestId,
            principal.getEmployeeId(),
            body.decision(),
            body.decisionType(),
            body.managerJustification());
    return RaiseRequestResponse.from(request);
  }

  /** FR-008: manager sends the request back to the employee with a comment. */
  @PreAuthorize("hasRole('MANAGER')")
  @PostMapping("/raise-requests/{requestId}/send-back")
  public RaiseRequestResponse sendBack(
      @AuthenticationPrincipal EmployeeUserDetails principal,
      @PathVariable Long requestId,
      @Valid @RequestBody SendBackRequest body) {
    var request =
        raiseRequestService.sendBack(requestId, principal.getEmployeeId(), body.comment());
    return RaiseRequestResponse.from(request);
  }
}
