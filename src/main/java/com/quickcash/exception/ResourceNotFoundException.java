package com.quickcash.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found: " + id);
    }

    public ResourceNotFoundException(String resource, UUID id) {
        this(resource, id.toString());
    }
}
