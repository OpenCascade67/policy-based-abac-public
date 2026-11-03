package com.pap.common.exception;

/** The caller may not perform the operation, or presented invalid credentials (HTTP 403). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException() {
        this("You do not have permission to perform this action.");
    }

    public ForbiddenException(String message) {
        super(message);
    }
}
