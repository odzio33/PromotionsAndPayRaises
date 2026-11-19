package com.example.payraisemodule.repository;

import com.example.payraisemodule.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Long> {
}
