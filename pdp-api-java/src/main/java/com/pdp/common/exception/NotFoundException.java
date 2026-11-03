package com.pdp.common.exception;

/** The requested entity does not exist (HTTP 404). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String entity, Object key) {
        super("%s with id '%s' was not found.".formatted(entity, key));
    }
}
