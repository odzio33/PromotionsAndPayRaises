package com.example.payraisemodule.controller;

import com.example.payraisemodule.dto.DecisionRequest;
import com.example.payraisemodule.dto.RaiseRequestResponse;
import com.example.payraisemodule.dto.SendBackRequest;
import com.example.payraisemodule.dto.SubmitRaiseRequest;
import com.example.payraisemodule.service.RaiseRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Covers FR-001 through FR-008 for the employee -> manager leg (MVP scope;
 * HR/CEO stages are out of scope per PRD ## Non-Goals).
 *
 * NOTE: {employeeId}/{managerId} are taken as path variables for now because
 * authentication (PRD ## Access Control: login required) is not wired up yet.
 * Once Spring Security is added, these should come from the authenticated
 * principal instead of the URL.
 */
@RestController
@RequestMapping("/api")
public class RaiseRequestController {

    private final RaiseRequestService raiseRequestService;

    public RaiseRequestController(RaiseRequestService raiseRequestService) {
        this.raiseRequestService = raiseRequestService;
    }

    /** FR-001: employee submits a raise request. */
    @PostMapping("/employees/{employeeId}/raise-requests")
    public ResponseEntity<RaiseRequestResponse> submit(
            @PathVariable Long employeeId,
            @Valid @RequestBody SubmitRaiseRequest body) {
        var request = raiseRequestService.submit(employeeId, body.requestedAmount(), body.justification());
        return ResponseEntity.status(HttpStatus.CREATED).body(RaiseRequestResponse.from(request));
    }

    /** FR-003: employee views their own request status and history. */
    @GetMapping("/employees/{employeeId}/raise-requests")
    public List<RaiseRequestResponse> findForEmployee(@PathVariable Long employeeId) {
        return raiseRequestService.findForEmployee(employeeId).stream()
                .map(RaiseRequestResponse::from)
                .toList();
    }

    /** FR-004/FR-005: manager views pending requests from direct reports, with band + history available on each. */
    @GetMapping("/managers/{managerId}/raise-requests/pending")
    public List<RaiseRequestResponse> findPendingForManager(@PathVariable Long managerId) {
        return raiseRequestService.findPendingForManager(managerId).stream()
                .map(RaiseRequestResponse::from)
                .toList();
    }

    /** FR-006/FR-007/FR-008: manager approves or rejects a request with a justification and band decision. */
    @PostMapping("/managers/{managerId}/raise-requests/{requestId}/decision")
    public RaiseRequestResponse decide(
            @PathVariable Long managerId,
            @PathVariable Long requestId,
            @Valid @RequestBody DecisionRequest body) {
        var request = raiseRequestService.decide(
                requestId, managerId, body.decision(), body.decisionType(), body.managerJustification());
        return RaiseRequestResponse.from(request);
    }

    /** FR-008: manager sends the request back to the employee with a comment. */
    @PostMapping("/managers/{managerId}/raise-requests/{requestId}/send-back")
    public RaiseRequestResponse sendBack(
            @PathVariable Long managerId,
            @PathVariable Long requestId,
            @Valid @RequestBody SendBackRequest body) {
        var request = raiseRequestService.sendBack(requestId, managerId, body.comment());
        return RaiseRequestResponse.from(request);
    }
}
