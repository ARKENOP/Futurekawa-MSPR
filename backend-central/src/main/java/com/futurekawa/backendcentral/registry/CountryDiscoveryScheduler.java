package com.futurekawa.backendcentral.registry;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.futurekawa.backendcentral.client.LocalBackendClient;
import com.futurekawa.lib.dto.response.PaysResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class CountryDiscoveryScheduler implements ApplicationRunner {

    private final CountryRegistry countryRegistry;

    @Override
    public void run(ApplicationArguments args) {
        refresh();
    }

    @Scheduled(fixedDelayString = "${futurekawa.discovery.fixed-delay-ms:60000}")
    public void refresh() {
        for (LocalBackendDescriptor descriptor : countryRegistry.all()) {
            LocalBackendClient client = countryRegistry.client(descriptor.codePays()).orElseThrow();
            try {
                PaysResponse pays = client.getPays();
                if (pays != null && pays.codePays() != null
                        && !pays.codePays().equals(descriptor.codePays())) {
                    log.error("Registry mismatch: {} is configured for '{}' but reports '{}'. "
                                    + "Fix futurekawa.locals or that backend's COUNTRY_CODE.",
                            descriptor.url(), descriptor.codePays(), pays.codePays());
                }
                descriptor.updatePays(pays);
            } catch (Exception e) {
                log.warn("Discovery failed for {} ({}): {}",
                        descriptor.codePays(), descriptor.url(), e.getMessage());
            }
        }
    }
}
