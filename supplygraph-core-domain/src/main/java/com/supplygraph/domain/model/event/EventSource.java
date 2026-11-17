package com.supplygraph.domain.model.event;

/**
 * Origin source classification for operational events.
 */
public enum EventSource {
    INTERNAL_WORKFLOW(true),
    MANUAL_OPERATOR(true),
    CSV_IMPORT(false),
    JSON_IMPORT(false),
    EXTERNAL_INTEGRATION(false),
    SYNTHETIC_SCENARIO(false);

    private final boolean authoritativeDefault;

    EventSource(boolean authoritativeDefault) {
        this.authoritativeDefault = authoritativeDefault;
    }

    public boolean isAuthoritativeDefault() {
        return authoritativeDefault;
    }
}

