package com.futurekawa.backendcentral.registry;

import java.util.concurrent.atomic.AtomicReference;

import com.futurekawa.backendcentral.dto.response.Pays;

/**
 * État courant connu d'un backend local : sa config statique (codePays attendu, url)
 * plus le dernier Pays reçu via GET /api/v1/pays (nomPays, seuils), rafraîchi par
 * CountryDiscoveryScheduler. `pays` est null tant que la première découverte n'a pas
 * réussi (le local n'est alors pas encore considéré "connu" pour l'agrégation).
 */
public final class LocalBackendDescriptor {

    private final String codePays;
    private final String url;
    private final AtomicReference<Pays> pays = new AtomicReference<>();

    public LocalBackendDescriptor(String codePays, String url) {
        this.codePays = codePays;
        this.url = url;
    }

    public String codePays() {
        return codePays;
    }

    public String url() {
        return url;
    }

    public Pays pays() {
        return pays.get();
    }

    public void updatePays(Pays latest) {
        pays.set(latest);
    }

    public String nomPays() {
        Pays latest = pays.get();
        return latest != null ? latest.nomPays() : codePays;
    }
}
