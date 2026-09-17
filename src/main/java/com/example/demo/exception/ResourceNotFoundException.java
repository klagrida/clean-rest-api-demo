package com.example.demo.exception;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " with id " + id + " not found");
    }
}
