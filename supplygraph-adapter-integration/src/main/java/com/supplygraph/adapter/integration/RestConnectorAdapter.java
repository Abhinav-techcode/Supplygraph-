package com.supplygraph.adapter.integration;

import com.supplygraph.application.connector.*;
import com.supplygraph.domain.model.connector.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * Real HTTP(S) adapter. GET {baseUri}/health for connectivity, GET {baseUri}/records for synchronization.
 * Bearer token comes from the credential reference at call time. No redirects, bounded body, per-request timeout, SSRF guard.
 * Verified only against a local test server (see RestConnectorAdapterTest); no vendor ERP has been exercised.
 */
@Component
public class RestConnectorAdapter implements ConnectorAdapter {
    public static final String TYPE = "rest";
    private static final Logger log = LoggerFactory.getLogger(RestConnectorAdapter.class);

    private final CredentialResolver credentials;
    private final RecordPayloadInspector inspector;
    private final OutboundUrlGuard guard;
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build();

    @Autowired
    public RestConnectorAdapter(CredentialResolver credentials, RecordPayloadInspector inspector) {
        this(credentials, inspector, new OutboundUrlGuard(false));
    }

    public RestConnectorAdapter(CredentialResolver credentials, RecordPayloadInspector inspector, OutboundUrlGuard guard) {
        this.credentials = credentials;
        this.inspector = inspector;
        this.guard = guard;
    }

    @Override public String adapterType() { return TYPE; }

    @Override public ConnectorCallResult testConnectivity(Connector c) {
        return call(c, "health", false);
    }

    @Override public ConnectorCallResult synchronize(Connector c) {
        return call(c, "records", true);
    }

    private ConnectorCallResult call(Connector c, String path, boolean parseRecords) {
        ConnectorConfiguration cfg = c.configuration();
        if (c.isSimulated() || cfg == null || cfg.baseUri() == null || cfg.credential() == null) {
            return ConnectorCallResult.failure(ErrorCategory.POLICY_BLOCKED, "REST adapter requires a configured LIVE connector", false);
        }
        String base = cfg.baseUri().toString();
        URI target = URI.create(base.endsWith("/") ? base + path : base + "/" + path);
        try {
            guard.check(target, cfg.allowedHosts());
        } catch (SecurityException e) {
            return ConnectorCallResult.failure(ErrorCategory.POLICY_BLOCKED, e.getMessage(), false);
        }
        byte[] token = credentials.resolve(cfg.credential()).orElse(null);
        if (token == null) return ConnectorCallResult.failure(ErrorCategory.AUTHENTICATION, "Credential reference could not be resolved", false);

        HttpRequest request = HttpRequest.newBuilder(target).GET().timeout(Duration.ofMillis(cfg.timeoutMillis()))
                .header("Authorization", "Bearer " + new String(token, java.nio.charset.StandardCharsets.UTF_8))
                .header("Accept", "application/json").build();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int status = response.statusCode();
            byte[] body;
            try (InputStream in = response.body()) { body = readBounded(in, cfg.maxResponseBytes()); }
            if (body == null) return ConnectorCallResult.failure(ErrorCategory.RESPONSE_TOO_LARGE, "Response exceeded " + cfg.maxResponseBytes() + " bytes", false);
            if (status == 401 || status == 403) return ConnectorCallResult.failure(ErrorCategory.AUTHENTICATION, "Remote rejected credentials (HTTP " + status + ")", false);
            if (status == 429 || status >= 500) return ConnectorCallResult.failure(ErrorCategory.REJECTED_BY_REMOTE, "Remote unavailable (HTTP " + status + ")", true);
            if (status >= 300) return ConnectorCallResult.failure(ErrorCategory.REJECTED_BY_REMOTE, "Unexpected HTTP " + status, false);
            if (!parseRecords) return ConnectorCallResult.ok(0, 0, 0, "HTTP " + status);
            try {
                RecordPayloadInspector.Inspection i = inspector.inspect(body);
                return ConnectorCallResult.ok(i.received(), i.valid(), i.rejected(), "HTTP " + status, inspector.parse(body));
            } catch (IllegalArgumentException e) {
                return ConnectorCallResult.failure(ErrorCategory.INVALID_RESPONSE, e.getMessage(), false);
            }
        } catch (HttpTimeoutException e) {
            return ConnectorCallResult.failure(ErrorCategory.TIMEOUT, "Request timed out after " + cfg.timeoutMillis() + " ms", true);
        } catch (IOException e) {
            log.warn("Connector {} network failure: {}", c.id(), e.getClass().getSimpleName());
            return ConnectorCallResult.failure(ErrorCategory.NETWORK, "Network failure", true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ConnectorCallResult.failure(ErrorCategory.UNKNOWN, "Interrupted", false);
        }
    }

    /** @return the body, or null if it exceeds {@code max}. */
    static byte[] readBounded(InputStream in, long max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > max) return null;
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
