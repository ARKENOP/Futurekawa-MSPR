package com.futurekawa.backendlocal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class OpenApiExportTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generateOpenApiSpec() throws Exception {
        // Fetch the OpenAPI specification in YAML format from the springdoc endpoint
        String openApiYaml = mockMvc.perform(get("/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Ensure the api directory exists
        Path apiDir = Paths.get("api");
        if (!Files.exists(apiDir)) {
            Files.createDirectories(apiDir);
        }

        // Write to backend-local/api/openapi.yml
        Path file = apiDir.resolve("openapi.yml");
        Files.writeString(file, openApiYaml);
        
        System.out.println("Successfully generated OpenAPI specification at: " + file.toAbsolutePath());
    }
}
