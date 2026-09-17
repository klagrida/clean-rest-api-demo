package com.example.demo.exception;

// Base for all "expected" business errors. The service layer throws these
// and knows nothing about HTTP - the mapping to status codes lives only
// in GlobalExceptionHandler.
public abstract class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
