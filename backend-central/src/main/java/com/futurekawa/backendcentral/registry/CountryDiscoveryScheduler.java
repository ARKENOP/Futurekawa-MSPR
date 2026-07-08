package com.futurekawa.backendcentral.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.futurekawa.backendcentral.client.LocalBackendClient;
import com.futurekawa.backendcentral.dto.response.Pays;

/**
 * Découvre / rafraîchit codePays+nomPays+seuils de chaque backend local via
 * GET {local}/api/v1/pays (§2 du contrat), au démarrage puis périodiquement.
 */
@Component
public class CountryDiscoveryScheduler implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CountryDiscoveryScheduler.class);

    private final CountryRegistry countryRegistry;

    public CountryDiscoveryScheduler(CountryRegistry countryRegistry) {
        this.countryRegistry = countryRegistry;
    }

    @Override
    public void run(ApplicationArguments args) {
        refresh();
    }

    @Scheduled(fixedDelayString = "${futurekawa.discovery.fixed-delay-ms:60000}")
    public void refresh() {
        for (LocalBackendDescriptor descriptor : countryRegistry.all()) {
            LocalBackendClient client = countryRegistry.client(descriptor.codePays()).orElseThrow();
            try {
                Pays pays = client.getPays();
                descriptor.updatePays(pays);
            } catch (Exception e) {
                log.warn("Découverte impossible pour {} ({}) : {}",
                        descriptor.codePays(), descriptor.url(), e.getMessage());
            }
        }
    }
}
