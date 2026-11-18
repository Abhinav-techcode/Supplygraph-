package com.supplygraph.adapter.integration;

import com.supplygraph.application.connector.CredentialResolver;
import com.supplygraph.domain.model.connector.CredentialReference;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/** Resolves {@code env:NAME} from the process environment. {@code vault:} references are intentionally unsupported here. */
@Component
public class EnvCredentialResolver implements CredentialResolver {
    @Override
    public Optional<byte[]> resolve(CredentialReference ref) {
        if (!ref.value().startsWith("env:")) return Optional.empty();
        String v = System.getenv(ref.value().substring(4));
        return v == null || v.isEmpty() ? Optional.empty() : Optional.of(v.getBytes(StandardCharsets.UTF_8));
    }
}
