package com.example.payraisemodule.controller;

import com.example.payraisemodule.service.RaiseRequestService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler({
    RaiseRequestService.NoSuchEmployeeException.class,
    RaiseRequestService.NoSuchRaiseRequestException.class
  })
  public ResponseEntity<Map<String, String>> handleNotFound(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler({
    RaiseRequestService.YearlyLimitExceededException.class,
    RaiseRequestService.NotTheAssignedManagerException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<Map<String, String>> handleConflict(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
  }
}
