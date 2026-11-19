package com.example.payraisemodule.repository;

import com.example.payraisemodule.domain.RaiseRequest;
import com.example.payraisemodule.domain.RequestStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaiseRequestRepository extends JpaRepository<RaiseRequest, Long> {

  List<RaiseRequest> findByEmployeeIdOrderBySubmittedAtDesc(Long employeeId);

  List<RaiseRequest> findByEmployeeManagerIdAndStatusOrderBySubmittedAtAsc(
      Long managerId, RequestStatus status);

  /** FR-002: only a completed (APPROVED/REJECTED) request counts against the yearly limit. */
  boolean existsByEmployeeIdAndStatusInAndDecidedAtAfter(
      Long employeeId, List<RequestStatus> statuses, Instant since);
}
