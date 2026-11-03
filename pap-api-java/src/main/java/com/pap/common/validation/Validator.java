package com.pap.common.validation;

import com.pap.common.exception.ValidationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fluent business-rule validation. Error keys and messages follow FluentValidation's conventions
 * (e.g. {@code "Request.Name": ["'Request Name' must not be empty."]}) so clients see the same errors
 * from either back end.
 *
 * <pre>{@code
 * Validator.create()
 *     .notEmpty("Request.Name", request.name())
 *     .maxLength("Request.Name", request.name(), 100)
 *     .validate();
 * }</pre>
 */
public final class Validator {

    private static final UUID EMPTY_UUID = new UUID(0L, 0L);

    private final Map<String, List<String>> errors = new LinkedHashMap<>();

    private Validator() {
    }

    public static Validator create() {
        return new Validator();
    }

    public Validator notEmpty(String property, String value) {
        if (value == null || value.isBlank()) add(property, "'%s' must not be empty.".formatted(display(property)));
        return this;
    }

    public Validator notEmpty(String property, UUID value) {
        if (value == null || EMPTY_UUID.equals(value)) add(property, "'%s' must not be empty.".formatted(display(property)));
        return this;
    }

    public Validator maxLength(String property, String value, int max) {
        if (value != null && value.length() > max) {
            add(property, "The length of '%s' must be %d characters or fewer. You entered %d characters."
                    .formatted(display(property), max, value.length()));
        }
        return this;
    }

    public Validator minLength(String property, String value, int min) {
        if (value != null && value.length() < min) {
            add(property, "The length of '%s' must be at least %d characters. You entered %d characters."
                    .formatted(display(property), min, value.length()));
        }
        return this;
    }

    public Validator exactLength(String property, String value, int length) {
        if (value != null && value.length() != length) {
            add(property, "'%s' must be %d characters in length. You entered %d characters."
                    .formatted(display(property), length, value.length()));
        }
        return this;
    }

    /** Same leniency as ASP.NET Core's email check: exactly one '@', neither first nor last. */
    public Validator email(String property, String value) {
        if (value != null) {
            int at = value.indexOf('@');
            if (at <= 0 || at == value.length() - 1 || at != value.lastIndexOf('@')) {
                add(property, "'%s' is not a valid email address.".formatted(display(property)));
            }
        }
        return this;
    }

    public Validator must(String property, boolean condition) {
        if (!condition) add(property, "The specified condition was not met for '%s'.".formatted(display(property)));
        return this;
    }

    /** @throws ValidationException if any rule failed */
    public void validate() {
        if (!errors.isEmpty()) throw new ValidationException(errors);
    }

    private void add(String property, String message) {
        errors.computeIfAbsent(property, p -> new ArrayList<>()).add(message);
    }

    /** "Request.ColorScheme" -> "Request Color Scheme". */
    private static String display(String property) {
        return property.replace('.', ' ').replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
    }
}
