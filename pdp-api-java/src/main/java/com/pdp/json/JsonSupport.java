package com.pdp.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/** Jackson settings for reading the PAP's JSON data files. */
public final class JsonSupport {

    private JsonSupport() {
    }

    /** Reads the PAP file format: camelCase enums, case-insensitive properties, unknown properties ignored. */
    public static ObjectMapper storageMapper() {
        return JsonMapper.builder()
                .addModule(new EnumNamingModule(EnumNamingModule.Style.CAMEL))
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false)
                .build();
    }
}
