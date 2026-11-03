package com.pap.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger, OpenAPI document at /swagger/v1/swagger.json (same paths as the .NET API). */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(title = "PAP API", version = "v1"), security = @SecurityRequirement(name = "Bearer"))
@SecurityScheme(name = "Bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
        description = "JWT access token from /api/auth/login")
public class OpenApiConfig {
}
