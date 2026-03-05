package com.nabra.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI nabraOpenApi() {
    final String schemeName = "bearerAuth";
    return new OpenAPI()
        .info(new Info().title("Nabra Backend API").version("v1").description("Backend APIs for Nabra (نبرة)"))
        .addSecurityItem(new SecurityRequirement().addList(schemeName))
        .components(new io.swagger.v3.oas.models.Components().addSecuritySchemes(
            schemeName,
            new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
        ));
  }
}
