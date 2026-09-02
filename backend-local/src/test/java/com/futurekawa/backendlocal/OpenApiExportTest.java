package com.futurekawa.backendlocal;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OpenApiExportTest extends AbstractIntegrationTest {
    @Test
    void generateOpenApiSpec() throws Exception {
        ResponseEntity<String> response = client.get()
                .uri("/api-docs.yaml")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("openapi:");

        Path apiDir = Paths.get("api");
        if (!Files.exists(apiDir)) {
            Files.createDirectories(apiDir);
        }
        Path file = apiDir.resolve("openapi.yml");
        Files.writeString(file, response.getBody());
        System.out.println("Generated OpenAPI specification at: " + file.toAbsolutePath());
    }
}
