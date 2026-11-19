package com.example.payraisemodule.service;

import com.example.payraisemodule.domain.DecisionType;
import com.example.payraisemodule.domain.Employee;
import com.example.payraisemodule.domain.Position;
import com.example.payraisemodule.domain.RaiseRequest;
import com.example.payraisemodule.domain.RequestStatus;
import com.example.payraisemodule.domain.Role;
import com.example.payraisemodule.repository.EmployeeRepository;
import com.example.payraisemodule.repository.RaiseRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaiseRequestServiceTest {

    @Mock
    private RaiseRequestRepository raiseRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private RaiseRequestService service;

    private Position position;
    private Employee manager;
    private Employee employee;

    @BeforeEach
    void setUp() {
        service = new RaiseRequestService(raiseRequestRepository, employeeRepository);
        position = new Position("Backend Developer", new BigDecimal("8000"), new BigDecimal("12000"));
        manager = withId(new Employee("manager@example.com", "Mo Manager", Role.MANAGER, null, position), 1L);
        employee = withId(new Employee("employee@example.com", "Ann Employee", Role.EMPLOYEE, manager, position), 2L);
    }

    @Test
    void submitSavesANewPendingRequestWhenNoCompletedRequestExistsThisYear() {
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(raiseRequestRepository.existsByEmployeeIdAndStatusInAndDecidedAtAfter(anyLong(), any(), any()))
                .thenReturn(false);
        when(raiseRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RaiseRequest result = service.submit(2L, new BigDecimal("11000"), "Shipped the migration.");

        assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(result.getEmployee()).isEqualTo(employee);
    }

    @Test
    void submitRejectsWhenEmployeeAlreadyHasACompletedRequestThisYear() {
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
        when(raiseRequestRepository.existsByEmployeeIdAndStatusInAndDecidedAtAfter(anyLong(), any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submit(2L, new BigDecimal("11000"), "Another attempt."))
                .isInstanceOf(RaiseRequestService.YearlyLimitExceededException.class);
    }

    @Test
    void submitRejectsForUnknownEmployee() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(99L, new BigDecimal("11000"), "..."))
                .isInstanceOf(RaiseRequestService.NoSuchEmployeeException.class);
    }

    @Test
    void decideSucceedsWhenCallerIsTheAssignedManager() {
        RaiseRequest request = new RaiseRequest(employee, new BigDecimal("11000"), "Shipped the migration.");
        when(raiseRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        RaiseRequest result = service.decide(10L, 1L, RequestStatus.APPROVED, DecisionType.WITHIN_BAND, "Good case.");

        assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    void decideRejectsWhenCallerIsNotTheAssignedManager() {
        RaiseRequest request = new RaiseRequest(employee, new BigDecimal("11000"), "Shipped the migration.");
        when(raiseRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.decide(10L, 999L, RequestStatus.APPROVED, DecisionType.WITHIN_BAND, "Good case."))
                .isInstanceOf(RaiseRequestService.NotTheAssignedManagerException.class);
    }

    @Test
    void sendBackSucceedsWhenCallerIsTheAssignedManager() {
        RaiseRequest request = new RaiseRequest(employee, new BigDecimal("11000"), "Shipped the migration.");
        when(raiseRequestRepository.findById(10L)).thenReturn(Optional.of(request));

        RaiseRequest result = service.sendBack(10L, 1L, "Please add more detail.");

        assertThat(result.getStatus()).isEqualTo(RequestStatus.SENT_BACK);
    }

    @Test
    void findPendingForManagerDelegatesToRepository() {
        when(raiseRequestRepository.findByEmployeeManagerIdAndStatusOrderBySubmittedAtAsc(1L, RequestStatus.PENDING))
                .thenReturn(List.of(new RaiseRequest(employee, new BigDecimal("11000"), "Shipped the migration.")));

        List<RaiseRequest> result = service.findPendingForManager(1L);

        assertThat(result).hasSize(1);
    }

    /** Test-only helper: entity ids are assigned by JPA, so reflection sets them for pre-persist fixtures. */
    private static Employee withId(Employee employee, Long id) {
        try {
            Field idField = Employee.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(employee, id);
            return employee;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
