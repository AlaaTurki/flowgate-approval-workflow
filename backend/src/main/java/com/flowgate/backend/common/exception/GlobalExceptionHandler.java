package com.flowgate.backend.common.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", "Validation failed");
        body.put("errors", errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, ConstraintViolationException.class,
            IllegalArgumentException.class})
    public ResponseEntity<Object> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        String msg = "Bad request";

        if (ex instanceof MethodArgumentTypeMismatchException) {
            MethodArgumentTypeMismatchException matm = (MethodArgumentTypeMismatchException) ex;
            String name = matm.getName();
            fieldErrors.put(name, "Invalid value for parameter");
            msg = "Invalid parameter";
        } else if (ex instanceof MissingServletRequestParameterException) {
            MissingServletRequestParameterException msrp = (MissingServletRequestParameterException) ex;
            fieldErrors.put(msrp.getParameterName(), "Missing required parameter");
            msg = "Missing parameter";
        } else if (ex instanceof ConstraintViolationException) {
            ConstraintViolationException cve = (ConstraintViolationException) ex;
            cve.getConstraintViolations().forEach(cv -> {
                String path = cv.getPropertyPath().toString();
                fieldErrors.put(path, cv.getMessage());
            });
            msg = "Validation failed";
        } else if (ex instanceof HttpMessageNotReadableException) {
            msg = "Malformed JSON request";
            Throwable cause = ex.getCause();
            try {
                if (cause != null && cause.getClass().getName().equals("com.fasterxml.jackson.databind.exc.InvalidFormatException")) {
                    // reflectively extract path and target type
                    com.fasterxml.jackson.databind.exc.InvalidFormatException ife = (com.fasterxml.jackson.databind.exc.InvalidFormatException) cause;
                    String field = ife.getPath() != null && !ife.getPath().isEmpty() ? ife.getPath().get(0).getFieldName() : "body";
                    Class<?> target = ife.getTargetType();
                    String detail = "Invalid value";
                    if (target != null && java.util.UUID.class.isAssignableFrom(target)) {
                        detail = "Malformed UUID";
                    } else if (target != null && target.isEnum()) {
                        detail = "Unknown enum value";
                    } else if (target != null && Number.class.isAssignableFrom(target)) {
                        detail = "Invalid number format";
                    }
                    fieldErrors.put(field, detail);
                }
            } catch (Exception ignore) {
                // fall through
            }
        } else if (ex instanceof IllegalArgumentException) {
            fieldErrors.put("", ex.getMessage() == null ? "Invalid argument" : ex.getMessage());
            msg = "Bad request";
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", msg);
        body.put("errors", fieldErrors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbidden(ForbiddenException ex) {
        log.warn("Forbidden: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage() == null ? "Forbidden" : ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage() == null ? "Forbidden" : ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleNotFound(NotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage() == null ? "Not found" : ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler({InvalidStateException.class, ConflictException.class, OptimisticLockException.class,
            OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<Object> handleConflict(Exception ex) {
        log.warn("Conflict/state error: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage() == null ? "Conflict" : ex.getMessage());
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<Object> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {}", ex.getMessage());
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage() == null ? "Method not allowed" : ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleAuth(AuthenticationException ex) {
        log.warn("Authentication error: {}", ex.getMessage());
        return build(HttpStatus.UNAUTHORIZED, "Authentication failed");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAny(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
    }

    private ResponseEntity<Object> build(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value());
        body.put("message", message);
        // always include errors map for consistency; field-level errors populate in handleValidation
        body.put("errors", new HashMap<>());
        return new ResponseEntity<>(body, status);
    }
}
