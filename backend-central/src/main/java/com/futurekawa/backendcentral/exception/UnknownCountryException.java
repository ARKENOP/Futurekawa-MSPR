package com.futurekawa.backendcentral.exception;

import lombok.Getter;

@Getter
public class UnknownCountryException extends RuntimeException {

    private final String codePays;

    public UnknownCountryException(String codePays) {
        super("Unknown country: " + codePays);
        this.codePays = codePays;
    }
}
