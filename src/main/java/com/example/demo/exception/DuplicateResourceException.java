package com.example.demo.exception;

public class DuplicateResourceException extends ApiException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
