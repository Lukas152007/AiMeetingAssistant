package com.example.meetingassistant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Map;

/** Returns actionable validation errors to the web UI instead of Spring's generic error page. */
@RestControllerAdvice
class ApiErrorHandler {
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<Map<String, Object>> responseStatus(ResponseStatusException error, HttpServletRequest request) {
    return ResponseEntity.status(error.getStatusCode()).body(Map.of(
      "timestamp", Instant.now().toString(),
      "status", error.getStatusCode().value(),
      "error", error.getStatusCode().toString(),
      "message", error.getReason() == null ? "Zahteva ni veljavna." : error.getReason(),
      "path", request.getRequestURI()
    ));
  }
}
