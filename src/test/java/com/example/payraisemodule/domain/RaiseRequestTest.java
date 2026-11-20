package com.example.payraisemodule.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RaiseRequestTest {

  private final Position position =
      new Position("Backend Developer", new BigDecimal("8000"), new BigDecimal("12000"));
  private final Employee employee =
      new Employee("employee@example.com", "Ann Employee", Role.EMPLOYEE, null, position, "hash");

  private RaiseRequest newRequest() {
    return new RaiseRequest(
        employee, new BigDecimal("11000"), "Shipped the Q3 migration, led two juniors.");
  }

  @Test
  void newRequestStartsPending() {
    RaiseRequest request = newRequest();

    assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
    assertThat(request.isCompleted()).isFalse();
  }

  @Test
  void decideApprovedRequiresJustification() {
    RaiseRequest request = newRequest();

    assertThatThrownBy(() -> request.decide(RequestStatus.APPROVED, DecisionType.WITHIN_BAND, " "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void decideApprovedRecordsJustificationAndCompletesTheRequest() {
    RaiseRequest request = newRequest();

    request.decide(RequestStatus.APPROVED, DecisionType.WITHIN_BAND, "Solid case, within band.");

    assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
    assertThat(request.getDecisionType()).isEqualTo(DecisionType.WITHIN_BAND);
    assertThat(request.getManagerJustification()).isEqualTo("Solid case, within band.");
    assertThat(request.getDecidedAt()).isNotNull();
    assertThat(request.isCompleted()).isTrue();
  }

  @Test
  void decideRejectsSendBackStatus() {
    RaiseRequest request = newRequest();

    assertThatThrownBy(
            () ->
                request.decide(
                    RequestStatus.SENT_BACK, DecisionType.WITHIN_BAND, "Needs more detail."))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void sendBackRequiresComment() {
    RaiseRequest request = newRequest();

    assertThatThrownBy(() -> request.sendBack("")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void sendBackRecordsCommentAndDoesNotCompleteTheRequest() {
    RaiseRequest request = newRequest();

    request.sendBack("Please document the completed goals.");

    assertThat(request.getStatus()).isEqualTo(RequestStatus.SENT_BACK);
    assertThat(request.getSendBackComment()).isEqualTo("Please document the completed goals.");
    assertThat(request.isCompleted()).isFalse();
  }
}
