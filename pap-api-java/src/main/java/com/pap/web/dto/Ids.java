package com.pap.web.dto;

import java.util.UUID;

/** Ids travel as strings in the API payloads. */
public final class Ids {

    private Ids() {
    }

    public static String format(UUID id) {
        return id == null ? null : id.toString();
    }

    /** @return {@code null} for a null or empty value; otherwise the parsed id */
    public static UUID parseOptional(String value) {
        return value == null || value.isEmpty() ? null : UUID.fromString(value.trim());
    }

    /** @throws IllegalArgumentException if {@code value} is not a GUID */
    public static UUID parse(String value) {
        if (value == null) throw new IllegalArgumentException("Id is required.");
        return UUID.fromString(value.trim());
    }
}
