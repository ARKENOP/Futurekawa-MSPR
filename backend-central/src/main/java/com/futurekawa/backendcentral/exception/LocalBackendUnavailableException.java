package com.futurekawa.backendcentral.exception;

/** Levée quand le backend local d'un pays est indisponible (circuit ouvert / timeout) pour une ressource unitaire (§5). */
public class LocalBackendUnavailableException extends RuntimeException {

    private final String codePays;

    public LocalBackendUnavailableException(String codePays, Throwable cause) {
        super("Backend local indisponible pour le pays " + codePays, cause);
        this.codePays = codePays;
    }

    public String codePays() {
        return codePays;
    }
}
