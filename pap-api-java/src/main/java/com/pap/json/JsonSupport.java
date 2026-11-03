package com.pap.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Instant;

/** Shared Jackson settings for the API payloads and the JSON data files. */
public final class JsonSupport {

    private JsonSupport() {
    }

    /** Lenient timestamp parsing, shared by both mappers. */
    public static SimpleModule timeModule() {
        return new SimpleModule("LenientInstant").addDeserializer(Instant.class, new LenientInstantDeserializer());
    }

    /**
     * Mapper for the JSON data files: camelCase enums, nulls omitted, indented like System.Text.Json
     * ({@code "key": value}, 2-space indents), so files are interchangeable with the .NET implementation.
     */
    public static ObjectMapper storageMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .addModule(timeModule())
                .addModule(new EnumNamingModule(EnumNamingModule.Style.CAMEL))
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .defaultPrettyPrinter(indentedPrinter())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    /** Pretty printer matching System.Text.Json's indented output. */
    public static DefaultPrettyPrinter indentedPrinter() {
        DefaultIndenter indenter = new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
        Separators separators = Separators.createDefaultInstance()
                .withObjectFieldValueSpacing(Separators.Spacing.AFTER)
                .withObjectEmptySeparator("")
                .withArrayEmptySeparator("");
        return new DefaultPrettyPrinter(separators)
                .withObjectIndenter(indenter)
                .withArrayIndenter(indenter);
    }
}
