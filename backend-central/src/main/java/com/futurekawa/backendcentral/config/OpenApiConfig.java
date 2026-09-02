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
                                Consolidated API aggregating the deployed country backend-local \
                                instances, one per country, listed in futurekawa.locals. \
                                No application-level authentication: the central is \
                                exposed on the headquarters' private network only \
                                (see docs/ARCHITECTURE.md).""")
                        .version("v1"));
    }
}
