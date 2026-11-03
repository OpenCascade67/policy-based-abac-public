package com.pap.json;

import java.util.Locale;

/**
 * Converts enum constants (UPPER_SNAKE_CASE) to the wire names used by the .NET implementation:
 * PascalCase in API payloads ({@code DENY_OVERRIDES -> "DenyOverrides"}) and camelCase in the JSON data files.
 */
public final class EnumNames {

    private EnumNames() {
    }

    public static String pascal(Enum<?> value) {
        StringBuilder sb = new StringBuilder();
        for (String part : value.name().split("_")) {
            if (part.isEmpty()) continue;
            sb.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }

    public static String camel(Enum<?> value) {
        String pascal = pascal(value);
        return pascal.isEmpty() ? pascal : Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    /** Lower-cased PascalCase name, e.g. {@code "PermitOverrides"} for {@code PERMIT_OVERRIDES}. */
    public static String lower(Enum<?> value) {
        return pascal(value).toLowerCase(Locale.ROOT);
    }

    /**
     * Exact (case-sensitive) lookup by PascalCase name, like .NET's {@code Enum.Parse}.
     *
     * @throws IllegalArgumentException if no constant has that name
     */
    public static <E extends Enum<E>> E parseExact(Class<E> type, String name) {
        for (E constant : type.getEnumConstants()) {
            if (pascal(constant).equals(name)) return constant;
        }
        throw new IllegalArgumentException("Requested value '%s' was not found in %s.".formatted(name, type.getSimpleName()));
    }

    /**
     * Case-insensitive lookup by PascalCase name or ordinal, mirroring System.Text.Json's JsonStringEnumConverter.
     *
     * @return the constant, or {@code null} when nothing matches
     */
    public static <E extends Enum<E>> E parse(Class<E> type, String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        E[] constants = type.getEnumConstants();
        if (trimmed.matches("-?\\d+")) {
            int ordinal = Integer.parseInt(trimmed);
            return ordinal >= 0 && ordinal < constants.length ? constants[ordinal] : null;
        }
        for (E constant : constants) {
            if (pascal(constant).equalsIgnoreCase(trimmed)) return constant;
        }
        return null;
    }
}
