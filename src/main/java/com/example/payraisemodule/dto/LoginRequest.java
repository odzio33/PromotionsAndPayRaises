package com.example.payraisemodule.dto;

import jakarta.validation.constraints.NotBlank;

/** Login request body for {@code POST /api/login}. */
public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
