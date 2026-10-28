package com.hospital.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// What is @RestControllerAdvice?
// This class WATCHES all your controllers.
// When any controller throws an exception —
// this class catches it automatically!
// Like a safety net under a trapeze artist.
//
// Without this:
// Exception thrown → ugly stack trace returned
//
// With this:
// Exception thrown → caught here →
// clean error response returned ✅
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ════════════════════════════════════════
    // Handle ResourceNotFoundException
    // ════════════════════════════════════════

    // What is @ExceptionHandler?
    // Tells Spring: "when THIS exception is thrown
    // anywhere in any controller →
    // run THIS method!"
    @ExceptionHandler(
            ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse>
            handleResourceNotFoundException(
            ResourceNotFoundException ex) {

        // Create clean error response
        ErrorResponse error = new ErrorResponse(
                404,
                ex.getMessage(),
                LocalDateTime.now(),
                "NOT_FOUND"
        );

        // Return 404 NOT FOUND status
        return new ResponseEntity<>(
                error, HttpStatus.NOT_FOUND);
    }

    // ════════════════════════════════════════
    // Handle @Valid failures
    // ════════════════════════════════════════

    // Thrown when request body breaks a rule
    // like @NotBlank or @Email on an entity.
    // We join all field messages into one.
    @ExceptionHandler(
            MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse>
            handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": "
                        + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = new ErrorResponse(
                400,
                message,
                LocalDateTime.now(),
                "VALIDATION_FAILED"
        );

        return new ResponseEntity<>(
                error, HttpStatus.BAD_REQUEST);
    }

    // ════════════════════════════════════════
    // Handle database constraint errors
    // ════════════════════════════════════════

    // Example: deleting a doctor who still
    // has appointments → foreign key error.
    @ExceptionHandler(
            DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse>
            handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {

        ErrorResponse error = new ErrorResponse(
                409,
                "Operation conflicts with existing "
                        + "data (e.g. record still has "
                        + "appointments)",
                LocalDateTime.now(),
                "CONFLICT"
        );

        return new ResponseEntity<>(
                error, HttpStatus.CONFLICT);
    }

    // ════════════════════════════════════════
    // Handle wrong email / password
    // ════════════════════════════════════════
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse>
            handleAuthenticationException(
            AuthenticationException ex) {

        ErrorResponse error = new ErrorResponse(
                401,
                "Invalid email or password",
                LocalDateTime.now(),
                "UNAUTHORIZED"
        );

        return new ResponseEntity<>(
                error, HttpStatus.UNAUTHORIZED);
    }

    // ════════════════════════════════════════
    // Handle "not your record"
    // ════════════════════════════════════════
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse>
            handleAccessDenied(
            AccessDeniedException ex) {

        ErrorResponse error = new ErrorResponse(
                403,
                ex.getMessage(),
                LocalDateTime.now(),
                "FORBIDDEN"
        );

        return new ResponseEntity<>(
                error, HttpStatus.FORBIDDEN);
    }

    // ════════════════════════════════════════
    // Handle unknown URLs
    // ════════════════════════════════════════

    // Example: /favicon.ico or a mistyped path.
    // This is a 404, not a server error.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse>
            handleNoResourceFound(
            NoResourceFoundException ex) {

        ErrorResponse error = new ErrorResponse(
                404,
                "Page not found: /"
                        + ex.getResourcePath(),
                LocalDateTime.now(),
                "NOT_FOUND"
        );

        return new ResponseEntity<>(
                error, HttpStatus.NOT_FOUND);
    }

    // ════════════════════════════════════════
    // Handle RuntimeException
    // ════════════════════════════════════════

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse>
            handleRuntimeException(
            RuntimeException ex) {

        ErrorResponse error = new ErrorResponse(
                400,
                ex.getMessage(),
                LocalDateTime.now(),
                "BAD_REQUEST"
        );

        return new ResponseEntity<>(
                error, HttpStatus.BAD_REQUEST);
    }

    // ════════════════════════════════════════
    // Handle ALL other exceptions
    // ════════════════════════════════════════

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse>
            handleGenericException(Exception ex) {

        ErrorResponse error = new ErrorResponse(
                500,
                "Something went wrong: "
                        + ex.getMessage(),
                LocalDateTime.now(),
                "INTERNAL_SERVER_ERROR"
        );

        return new ResponseEntity<>(
                error,
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}