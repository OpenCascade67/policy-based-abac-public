package com.pdp.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.pdp.json.EnumNamingModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API payload conventions shared with the PAP: camelCase properties (case-insensitive on input),
 * PascalCase enum values, nulls omitted, and decimal values kept exactly as sent.
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    @Bean
    Module pascalCaseEnumModule() {
        return new EnumNamingModule(EnumNamingModule.Style.PASCAL);
    }

    @Bean
    Jackson2ObjectMapperBuilderCustomizer pdpJacksonCustomizer() {
        return builder -> builder
                .featuresToEnable(
                        MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES,
                        DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .postConfigurer(mapper -> mapper.configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false));
    }
}
