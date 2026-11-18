package com.supplygraph.domain.model.connector;

import java.net.URI;
import java.util.List;
import java.util.Objects;

/** Validated, secret-free connector configuration. */
public record ConnectorConfiguration(
        URI baseUri,
        CredentialReference credential,
        List<String> allowedHosts,
        int timeoutMillis,
        long maxResponseBytes,
        int maxRetries
) {
    public ConnectorConfiguration {
        Objects.requireNonNull(allowedHosts, "allowedHosts must not be null");
        allowedHosts = List.copyOf(allowedHosts);
        if (timeoutMillis < 100 || timeoutMillis > 60_000) throw new IllegalArgumentException("timeoutMillis must be within 100..60000");
        if (maxResponseBytes < 1 || maxResponseBytes > 10_485_760L) throw new IllegalArgumentException("maxResponseBytes must be within 1..10485760");
        if (maxRetries < 0 || maxRetries > 10) throw new IllegalArgumentException("maxRetries must be within 0..10");
        if (baseUri != null) {
            String scheme = baseUri.getScheme();
            if (scheme == null || !(scheme.equals("https") || scheme.equals("http"))) {
                throw new IllegalArgumentException("baseUri scheme must be http or https");
            }
            if (baseUri.getHost() == null) throw new IllegalArgumentException("baseUri must have a host");
            if (baseUri.getUserInfo() != null) throw new IllegalArgumentException("baseUri must not embed credentials");
            if (!allowedHosts.contains(baseUri.getHost())) {
                throw new IllegalArgumentException("baseUri host '" + baseUri.getHost() + "' is not in the allowed host list");
            }
        }
    }
}
