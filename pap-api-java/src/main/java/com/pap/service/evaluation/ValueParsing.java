package com.pap.service.evaluation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Culture-invariant parsing of attribute values held as strings. */
final class ValueParsing {

    private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

    /** Date/time forms accepted for comparisons; values without an offset are UTC. */
    private static final List<Function<String, Instant>> DATE_PARSERS = List.of(
            s -> OffsetDateTime.parse(s).toInstant(),
            s -> LocalDateTime.parse(s).toInstant(ZoneOffset.UTC),
            s -> LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]")).toInstant(ZoneOffset.UTC),
            s -> LocalDate.parse(s).atStartOfDay().toInstant(ZoneOffset.UTC),
            s -> LocalDateTime.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy H:mm[:ss]", Locale.ROOT)).toInstant(ZoneOffset.UTC),
            s -> LocalDate.parse(s, DateTimeFormatter.ofPattern("M/d/yyyy", Locale.ROOT)).atStartOfDay().toInstant(ZoneOffset.UTC),
            s -> ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant(),
            // A bare time compares on today's date
            s -> LocalTime.parse(s).atDate(LocalDate.now(ZoneOffset.UTC)).toInstant(ZoneOffset.UTC));

    private ValueParsing() {
    }

    static OptionalDouble parseNumber(String text) {
        if (text == null) return OptionalDouble.empty();
        String s = text.trim();
        switch (s) {
            case "NaN" -> { return OptionalDouble.of(Double.NaN); }
            case "Infinity" -> { return OptionalDouble.of(Double.POSITIVE_INFINITY); }
            case "-Infinity" -> { return OptionalDouble.of(Double.NEGATIVE_INFINITY); }
            default -> {
                if (!DECIMAL.matcher(s).matches()) return OptionalDouble.empty();
                return OptionalDouble.of(Double.parseDouble(s));
            }
        }
    }

    static Optional<Boolean> parseBoolean(String text) {
        if (text == null) return Optional.empty();
        String s = text.trim();
        if (s.equalsIgnoreCase("true")) return Optional.of(true);
        if (s.equalsIgnoreCase("false")) return Optional.of(false);
        return Optional.empty();
    }

    static Optional<Instant> parseInstant(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        String s = text.trim();
        for (Function<String, Instant> parser : DATE_PARSERS) {
            try {
                return Optional.of(parser.apply(s));
            } catch (DateTimeParseException ignored) {
                // try the next form
            }
        }
        return Optional.empty();
    }
}
