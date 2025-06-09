package com.budget.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException e, WebRequest request) {
        HttpStatus status = e.getStatus() != null ? e.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR;
        
        if (status.is5xxServerError()) {
            log.error("API Exception ({}): {}", status.value(), e.getMessage(), e);
        } else {
            log.warn("API Exception ({}): {}", status.value(), e.getMessage(), e);
        }

        Map<String, Object> response = createErrorResponse(
            status.getReasonPhrase(),
            e.getMessage(),
            status,
            request.getDescription(false)
        );

        return ResponseEntity.status(status).body(response);
    }

    private Map<String, Object> createErrorResponse(String error, String message, HttpStatus status, String path) {
        Map<String, Object> response = new HashMap<>();

        response.put("success", false);
        response.put("error", error);
        response.put("message", message);
        response.put("status", status.value());
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("path", path);

        return response;
    }
}
