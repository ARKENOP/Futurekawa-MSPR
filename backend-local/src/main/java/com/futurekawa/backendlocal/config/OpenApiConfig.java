package com.futurekawa.backendlocal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI futurekawaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FutureKawa Backend Local API")
                        .description("REST API for local warehouse management, lot tracking, and IoT sensor metrics. (Deployed per country)")
                        .version("v0.1.0"));
    }
}
