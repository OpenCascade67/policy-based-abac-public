package com.pap.common.exception;

/** The operation conflicts with existing data, e.g. a duplicate name (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
