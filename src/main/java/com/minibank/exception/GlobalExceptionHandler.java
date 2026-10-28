package com.minibank.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.minibank.dto.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Malformed request body";
        if (ex.getCause() instanceof InvalidFormatException cause && cause.getTargetType().isEnum()) {
            message = "Invalid value '%s', expected one of %s"
                    .formatted(cause.getValue(), Arrays.toString(cause.getTargetType().getEnumConstants()));
        }
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Invalid value '%s' for parameter '%s'".formatted(ex.getValue(), ex.getName());
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    // Two concurrent registrations can both pass the existsByEmail check; the unique constraint catches the second one
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = "Request conflicts with existing data";
        if (ex.getCause() instanceof ConstraintViolationException cause
                && "uk_users_email".equals(cause.getConstraintName())) {
            message = "Email is already registered";
        }
        return build(HttpStatus.CONFLICT, message, request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest request) {
        // Standard Spring MVC exceptions (unknown URL, wrong HTTP method, missing header...) know their own status
        if (ex instanceof ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return build(status, springError.getBody().getDetail(), request, null);
        }
        log.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request,
                                           List<ApiError.FieldViolation> fieldErrors) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status).body(body);
    }
}
