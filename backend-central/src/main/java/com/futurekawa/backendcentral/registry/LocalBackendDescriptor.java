package com.futurekawa.backendcentral.registry;

import java.util.concurrent.atomic.AtomicReference;

import com.futurekawa.lib.dto.response.PaysResponse;

public final class LocalBackendDescriptor {
    private final String codePays;
    private final String url;
    private final AtomicReference<PaysResponse> pays = new AtomicReference<>();

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

    public PaysResponse pays() {
        return pays.get();
    }

    public void updatePays(PaysResponse latest) {
        pays.set(latest);
    }

    public String nomPays() {
        PaysResponse latest = pays.get();
        return latest != null ? latest.nomPays() : codePays;
    }
}
