package com.supplygraph.application.connector;

import com.supplygraph.domain.model.connector.CredentialReference;

import java.util.Optional;

/** Resolves a credential reference to secret bytes at call time. Secrets must never be logged, persisted or shown. */
public interface CredentialResolver {
    Optional<byte[]> resolve(CredentialReference reference);
}
