package com.hospital.exception;

// What is this?
// A custom exception class for our hospital app.
// We throw this when something is not found.
// Example: patient not found, doctor not found.
//
// extends RuntimeException means:
// This IS a RuntimeException
// (inherits all its features)
// but with OUR custom message!
public class ResourceNotFoundException
        extends RuntimeException {

    // Constructor that takes a message
    // When we throw this exception —
    // we pass the message like:
    // throw new ResourceNotFoundException(
    //     "Patient not found with id: 5")
    public ResourceNotFoundException(String message) {
        // super() calls the parent class
        // (RuntimeException) constructor
        // with our message
        super(message);
    }
}