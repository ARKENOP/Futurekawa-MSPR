package com.futurekawa.backendlocal.odoo;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.futurekawa.backendlocal.config.OdooProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * Client for interacting with Odoo via JSON-RPC.
 * Uses Spring Boot 3.2+ RestClient.
 */
@Slf4j
@Component
public class OdooRpcClient {

    private final RestClient restClient;
    private final OdooProperties odooProperties;
    private Integer cachedUid = null;

    public OdooRpcClient(RestClient.Builder restClientBuilder, OdooProperties odooProperties) {
        this.restClient = restClientBuilder.baseUrl(odooProperties.url()).build();
        this.odooProperties = odooProperties;
    }

    /**
     * Authenticates with Odoo and returns the user ID (UID).
     * Caches the UID to avoid repeated authentication calls.
     */
    public synchronized Integer authenticate() {
        if (cachedUid != null) {
            return cachedUid;
        }

        Map<String, Object> params = Map.of(
                "service", "common",
                "method", "authenticate",
                "args", List.of(
                        odooProperties.db(),
                        odooProperties.apiUser(),
                        odooProperties.apiKey(),
                        Map.of()
                )
        );

        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "method", "call",
                "params", params,
                "id", 1
        );

        Map<String, Object> response = restClient.post()
                .uri("/jsonrpc")
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response != null && response.containsKey("result")) {
            Object result = response.get("result");
            if (result instanceof Number num) {
                cachedUid = num.intValue();
                log.info("Successfully authenticated with Odoo as UID: {}", cachedUid);
                return cachedUid;
            }
        }

        throw new IllegalStateException("Failed to authenticate with Odoo. Response: " + response);
    }

    /**
     * Executes a method on an Odoo model via object/execute_kw.
     */
    public Object executeKw(String model, String method, List<Object> args, Map<String, Object> kwargs) {
        Integer uid = authenticate();

        Map<String, Object> params = Map.of(
                "service", "object",
                "method", "execute_kw",
                "args", List.of(
                        odooProperties.db(),
                        uid,
                        odooProperties.apiKey(),
                        model,
                        method,
                        args,
                        kwargs != null ? kwargs : Map.of()
                )
        );

        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "method", "call",
                "params", params,
                "id", 2
        );

        Map<String, Object> response = restClient.post()
                .uri("/jsonrpc")
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response != null && response.containsKey("error")) {
            throw new RuntimeException("Odoo RPC error: " + response.get("error"));
        }

        return response != null ? response.get("result") : null;
    }
}
