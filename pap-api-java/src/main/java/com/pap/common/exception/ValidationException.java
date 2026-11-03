package com.pap.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Business validation failed (HTTP 400); {@code errors} maps a property path to its messages. */
public class ValidationException extends RuntimeException {

    private final transient Map<String, List<String>> errors;

    public ValidationException(Map<String, List<String>> errors) {
        super("One or more validation errors occurred.");
        this.errors = Collections.unmodifiableMap(new LinkedHashMap<>(errors));
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }
}
