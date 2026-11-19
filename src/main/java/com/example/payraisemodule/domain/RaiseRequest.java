package com.example.payraisemodule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single raise/promotion request (PRD User Story US-01). MVP scope covers
 * only the employee -> manager leg; HR/CEO stages are out of scope (see PRD
 * ## Non-Goals).
 */
@Entity
@Table(name = "raise_requests")
public class RaiseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "requested_amount", nullable = false)
    private BigDecimal requestedAmount;

    @Lob
    @Column(nullable = false)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type")
    private DecisionType decisionType;

    @Lob
    @Column(name = "manager_justification")
    private String managerJustification;

    @Lob
    @Column(name = "send_back_comment")
    private String sendBackComment;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected RaiseRequest() {
        // JPA
    }

    public RaiseRequest(Employee employee, BigDecimal requestedAmount, String justification) {
        this.employee = employee;
        this.requestedAmount = requestedAmount;
        this.justification = justification;
        this.status = RequestStatus.PENDING;
        this.submittedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public String getJustification() {
        return justification;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public DecisionType getDecisionType() {
        return decisionType;
    }

    public String getManagerJustification() {
        return managerJustification;
    }

    public String getSendBackComment() {
        return sendBackComment;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    /** FR-006/FR-007/FR-008: manager approves or rejects with a justification and a within-band/promotion decision. */
    public void decide(RequestStatus decision, DecisionType decisionType, String managerJustification) {
        if (decision != RequestStatus.APPROVED && decision != RequestStatus.REJECTED) {
            throw new IllegalArgumentException("decide() only accepts APPROVED or REJECTED; use sendBack() otherwise");
        }
        if (managerJustification == null || managerJustification.isBlank()) {
            throw new IllegalArgumentException("A manager justification is required to approve or reject a request");
        }
        this.status = decision;
        this.decisionType = decisionType;
        this.managerJustification = managerJustification;
        this.decidedAt = Instant.now();
    }

    /** FR-008: manager sends the request back to the employee with a required comment. */
    public void sendBack(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("A comment is required to send a request back");
        }
        this.status = RequestStatus.SENT_BACK;
        this.sendBackComment = comment;
    }

    /** True once a request has reached a terminal outcome (FR-002: only these count against the yearly limit). */
    public boolean isCompleted() {
        return status == RequestStatus.APPROVED || status == RequestStatus.REJECTED;
    }
}
