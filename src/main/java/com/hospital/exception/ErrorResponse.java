package com.hospital.exception;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// What is this?
// This is the shape of error messages
// we send back to React/Angular.
// Every error response has these 4 fields.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    // HTTP status code
    // Example: 404, 400, 500
    private int status;

    // Human readable error message
    // Example: "Patient not found with id: 5"
    private String message;

    // When the error happened
    private LocalDateTime timestamp;

    // What type of error
    // Example: "NOT_FOUND", "BAD_REQUEST"
    private String error;
}