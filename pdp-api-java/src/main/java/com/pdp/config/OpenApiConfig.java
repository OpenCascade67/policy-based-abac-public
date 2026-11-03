package com.pdp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger, OpenAPI document at /swagger/v1/swagger.json (same layout as the PAP). */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(title = "PDP API", version = "v1",
        description = "Policy Decision Point for the policies administered in the PAP"),
        security = @SecurityRequirement(name = "ApiKey"))
@SecurityScheme(name = "ApiKey", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, paramName = "X-API-Key")
public class OpenApiConfig {
}
