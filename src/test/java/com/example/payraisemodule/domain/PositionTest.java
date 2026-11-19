package com.example.payraisemodule.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PositionTest {

    private final Position position = new Position("Backend Developer", new BigDecimal("8000"), new BigDecimal("12000"));

    @Test
    void amountWithinBandIsWithinBand() {
        assertThat(position.isWithinBand(new BigDecimal("10000"))).isTrue();
    }

    @Test
    void amountAtBandBoundariesIsWithinBand() {
        assertThat(position.isWithinBand(new BigDecimal("8000"))).isTrue();
        assertThat(position.isWithinBand(new BigDecimal("12000"))).isTrue();
    }

    @Test
    void amountBelowBandIsNotWithinBand() {
        assertThat(position.isWithinBand(new BigDecimal("7999.99"))).isFalse();
    }

    @Test
    void amountAboveBandIsNotWithinBand() {
        assertThat(position.isWithinBand(new BigDecimal("12000.01"))).isFalse();
    }
}
