package com.futurekawa.backendcentral.exception;

import lombok.Getter;

@Getter
public class LocalBackendUnavailableException extends RuntimeException {

    private final String codePays;

    public LocalBackendUnavailableException(String codePays, Throwable cause) {
        super("Backend local unavailable for country " + codePays, cause);
        this.codePays = codePays;
    }
}
