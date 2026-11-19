package com.example.payraisemodule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "positions")
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String title;

    @Column(name = "salary_band_min", nullable = false)
    private BigDecimal salaryBandMin;

    @Column(name = "salary_band_max", nullable = false)
    private BigDecimal salaryBandMax;

    protected Position() {
        // JPA
    }

    public Position(String title, BigDecimal salaryBandMin, BigDecimal salaryBandMax) {
        this.title = title;
        this.salaryBandMin = salaryBandMin;
        this.salaryBandMax = salaryBandMax;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getSalaryBandMin() {
        return salaryBandMin;
    }

    public void setSalaryBandMin(BigDecimal salaryBandMin) {
        this.salaryBandMin = salaryBandMin;
    }

    public BigDecimal getSalaryBandMax() {
        return salaryBandMax;
    }

    public void setSalaryBandMax(BigDecimal salaryBandMax) {
        this.salaryBandMax = salaryBandMax;
    }

    public boolean isWithinBand(BigDecimal amount) {
        return amount.compareTo(salaryBandMin) >= 0 && amount.compareTo(salaryBandMax) <= 0;
    }
}
