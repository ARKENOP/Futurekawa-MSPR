package com.futurekawa.backendcentral.exception;

/** Levée quand un codePays demandé (path/body) ne correspond à aucun backend local enregistré. */
public class UnknownCountryException extends RuntimeException {

    public UnknownCountryException(String codePays) {
        super("Pays inconnu : " + codePays);
    }
}
