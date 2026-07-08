package com.futurekawa.backendcentral.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Pool dédié à l'exécution parallèle des appels vers les backends locaux (fan-out). */
@Configuration
public class AsyncConfig {

    @Bean
    public ExecutorService fanoutTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
