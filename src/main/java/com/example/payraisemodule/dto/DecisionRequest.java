package com.example.payraisemodule.dto;

import com.example.payraisemodule.domain.DecisionType;
import com.example.payraisemodule.domain.RequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * FR-006/FR-007/FR-008: manager approves or rejects a request with a justification and a band
 * decision.
 */
public record DecisionRequest(
    @NotNull RequestStatus decision,
    @NotNull DecisionType decisionType,
    @NotBlank String managerJustification) {}
