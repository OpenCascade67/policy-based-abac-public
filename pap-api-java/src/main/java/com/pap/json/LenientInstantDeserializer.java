package com.pap.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Reads ISO-8601 timestamps with or without an offset. Values without an offset are treated as UTC,
 * matching how the .NET implementation stores {@code DateTime.UtcNow} values.
 */
public class LenientInstantDeserializer extends StdScalarDeserializer<Instant> {

    public LenientInstantDeserializer() {
        super(Instant.class);
    }

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        if (text == null || text.isBlank()) return null;
        String value = text.trim();
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException ignored) {
            // fall through to offset-less form
        }
        try {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException e) {
            return (Instant) ctxt.handleWeirdStringValue(Instant.class, value, "Invalid date-time value");
        }
    }
}
