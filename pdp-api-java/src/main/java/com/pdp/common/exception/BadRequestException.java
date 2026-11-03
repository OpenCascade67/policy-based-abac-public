package com.pdp.common.exception;

/** The request is well-formed JSON but cannot be processed as sent (HTTP 400). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
