package com.futurekawa.backendcentral.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI backendCentralOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FutureKawa Backend Central")
                        .description("""
                                Consolidated API aggregating the country backend-local instances \
                                (BR/EC/CO). No application-level authentication: the central is \
                                exposed on the headquarters' private network only \
                                (see docs/ARCHITECTURE.md).""")
                        .version("v1"));
    }
}
