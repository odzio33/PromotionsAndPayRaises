package com.example.payraisemodule.domain;

/**
 * Set by the manager when deciding a request (FR-007): whether the requested
 * amount fits the employee's current position band, or requires moving the
 * employee to a different position/band.
 */
public enum DecisionType {
    WITHIN_BAND,
    REQUIRES_PROMOTION
}
