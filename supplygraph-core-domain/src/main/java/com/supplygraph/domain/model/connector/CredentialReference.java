package com.supplygraph.domain.model.connector;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Opaque pointer to a secret held outside the database (e.g. {@code env:ERP_TOKEN}).
 * The platform never stores or displays secret material; only the reference is persisted.
 */
public record CredentialReference(String value) {
    private static final Pattern VALID = Pattern.compile("^(env|vault):[A-Za-z0-9_./-]{1,100}$");

    public CredentialReference {
        Objects.requireNonNull(value, "credential reference must not be null");
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Credential must be a reference of the form 'env:NAME' or 'vault:path'; raw secrets are not accepted");
        }
    }

    /** Safe for logs/UI: reveals only the reference, which is not itself a secret. */
    @Override public String toString() { return "CredentialReference[" + value + "]"; }
}
