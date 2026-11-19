package com.example.payraisemodule.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** FR-001: employee submits a request with a justification and a requested amount. */
public record SubmitRaiseRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal requestedAmount,
        @NotBlank String justification
) {
}
