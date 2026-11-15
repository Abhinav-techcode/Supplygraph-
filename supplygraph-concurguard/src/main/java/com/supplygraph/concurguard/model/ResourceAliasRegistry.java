package com.supplygraph.concurguard.model;

import java.util.*;

/**
 * Registry resolving informal agent-generated text references and legacy aliases
 * into canonical ResourceIdentity instances to prevent alias-hidden conflicts.
 */
public class ResourceAliasRegistry {

    private final Map<String, ResourceIdentity> aliasMap = new LinkedHashMap<>();

    public ResourceAliasRegistry() {
    }

    public synchronized void registerAlias(String alias, ResourceIdentity canonical) {
        Objects.requireNonNull(alias, "Alias must not be null");
        Objects.requireNonNull(canonical, "Canonical identity must not be null");
        aliasMap.put(normalizeKey(alias), canonical);
    }

    public synchronized Optional<ResourceIdentity> resolve(String rawReference) {
        if (rawReference == null || rawReference.isBlank()) {
            return Optional.empty();
        }
        ResourceIdentity identity = aliasMap.get(normalizeKey(rawReference));
        return Optional.ofNullable(identity);
    }

    public synchronized ResourceIdentity resolveOrFallback(String rawReference, String defaultType, String defaultFacility) {
        return resolve(rawReference).orElseGet(() ->
                ResourceIdentity.of(defaultType, rawReference.trim().toUpperCase(), defaultFacility));
    }

    private static String normalizeKey(String key) {
        return key.trim().replaceAll("[\\s_-]+", "").toUpperCase(Locale.ROOT);
    }
}
