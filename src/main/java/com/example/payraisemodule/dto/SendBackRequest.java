package com.example.payraisemodule.dto;

import jakarta.validation.constraints.NotBlank;

/** FR-008: manager sends the request back to the employee with a required comment. */
public record SendBackRequest(@NotBlank String comment) {}
